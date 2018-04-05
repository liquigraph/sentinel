package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.springframework.stereotype.Service

sealed class VersionChange {
    abstract fun newVersion(): SemanticVersion
}

data class Update(val old: SemanticVersion, val new: SemanticVersion, val dockerized: Boolean) : VersionChange() {
    override fun newVersion() = new
}

data class Addition(val new: SemanticVersion, val dockerized: Boolean) : VersionChange() {
    override fun newVersion() = new
}

private data class VersionBranch(val major: Int, val minor: Int) {
    constructor(version: SemanticVersion) : this(version.major, version.minor)
}

@Service
class LiquigraphService {

    fun computeChanges(existingVersionData: List<TravisNeo4jVersion>,
                       mavenArtifacts: List<MavenArtifact>,
                       dockerizedVersions: Set<SemanticVersion>): List<VersionChange> {


        val existingVersions = existingVersionData.map { it.version }.sortedDescending()
        val mininumExistingVersion = existingVersions.last()
        val upperBound = existingVersions.first().major
        return mavenArtifacts
                .mapNotNull { it.version }
                .filter { it.isStable() }
                .filterNot { it < mininumExistingVersion || it.major > upperBound }
                .sorted()
                .groupBy { VersionBranch(it) }
                .flatMap {
                    val existingInBranch = existingVersionData.filter { ex -> VersionBranch(ex.version) == it.key }
                    when {
                        existingInBranch.isNotEmpty() -> {
                            existingBranchChanges(it.value, existingInBranch, dockerizedVersions, mininumExistingVersion)
                        }
                        else ->
                            newBranchAdditions(it.value, dockerizedVersions)
                    }
                }
    }

    private fun existingBranchChanges(newVersions: List<SemanticVersion>,
                                      existingVersions: List<TravisNeo4jVersion>,
                                      dockerizedVersions: Set<SemanticVersion>,
                                      mininumExistingVersion: SemanticVersion): List<VersionChange> {

        val minimumInBranch = existingVersions.minBy { it.version }!!
        return when {
            !inSmallestBranch(minimumInBranch, mininumExistingVersion) ->
                computeChanges(newVersions, dockerizedVersions, existingVersions.maxBy { it.version })
            else -> {
                val existingButMinimum = existingVersions.drop(1).maxBy { it.version }
                val changes = computeChanges(newVersions.filterNot { it == mininumExistingVersion }, dockerizedVersions, existingButMinimum)
                if (!minimumInBranch.inDockerStore && dockerizedVersions.contains(mininumExistingVersion)) {
                    listOf(Update(mininumExistingVersion, mininumExistingVersion, dockerized = true)).plus(changes)
                } else changes
            }
        }
    }

    private fun inSmallestBranch(minimumInBranch: TravisNeo4jVersion, mininumExistingVersion: SemanticVersion) =
            minimumInBranch.version == mininumExistingVersion

    private fun computeChanges(newVersions: List<SemanticVersion>,
                               dockerizedVersions: Set<SemanticVersion>,
                               existing: TravisNeo4jVersion?): List<VersionChange> {

        val new = newVersions.lastOrNull()
        val newIsDockerized = dockerizedVersions.contains(new)
        return when {
            new == null -> listOf()
            existing == null -> listOf(Addition(new, newIsDockerized))
            new == existing.version ->
                if (!newIsDockerized || existing.inDockerStore) listOf()
                else {
                    listOf(Update(new, new, dockerized = true))
                }
            new > existing.version -> {
                computeUpdates(existing, newIsDockerized, new, newVersions, dockerizedVersions)
            }
            else -> listOf()
        }
    }

    private fun computeUpdates(existing: TravisNeo4jVersion, newIsDockerized: Boolean, new: SemanticVersion, newVersions: List<SemanticVersion>, dockerizedVersions: Set<SemanticVersion>): List<VersionChange> {
        return if (!existing.inDockerStore || newIsDockerized) {
            listOf(Update(existing.version, new, dockerized = newIsDockerized))
        } else {
            val addition = Addition(new, dockerized = false)
            val secondHighestNew = newVersions.lastOrNull { it > new && dockerizedVersions.contains(it) }
            if (secondHighestNew != null) {
                listOf(addition, Update(existing.version, secondHighestNew, dockerized = true))
            } else {
                listOf(addition)
            }
        }
    }

    private fun newBranchAdditions(newVersions: List<SemanticVersion>, dockerizedVersions: Set<SemanticVersion>): List<Addition> {
        val highestNew = newVersions.last()
        return if (dockerizedVersions.contains(highestNew)) listOf(Addition(highestNew, dockerized = true))
        else {
            val highestDockerized = newVersions.lastOrNull { it < highestNew && dockerizedVersions.contains(it) }
            if (highestDockerized == null) {
                listOf(Addition(highestNew, dockerized = false))
            } else {
                listOf(
                        Addition(highestNew, dockerized = false),
                        Addition(highestDockerized, dockerized = true)
                )
            }
        }
    }
}