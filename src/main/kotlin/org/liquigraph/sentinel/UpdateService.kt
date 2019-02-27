package org.liquigraph.sentinel

import org.liquigraph.sentinel.github.StoredVersion
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.springframework.stereotype.Service

sealed class VersionChange : Comparable<VersionChange> {
    abstract fun newVersion(): SemanticVersion

    override fun compareTo(other: VersionChange): Int {
        return newVersion().compareTo(other.newVersion())
    }
}

data class Update(val old: SemanticVersion, val new: SemanticVersion, val dockerized: Boolean) : VersionChange() {
    override fun newVersion() = new

    override fun toString(): String {
        return "$old -> $new | $dockerized"
    }
}

data class Addition(val new: SemanticVersion, val dockerized: Boolean) : VersionChange() {
    override fun newVersion() = new
    override fun toString(): String {
        return "$new | $dockerized"
    }
}

private data class MajorMinorBranch(val major: Int, val minor: Int) {
    constructor(version: SemanticVersion) : this(version.major, version.minor)
}

@Service
class UpdateService {

    fun computeVersionChanges(storedVersionData: List<StoredVersion>,
                              mavenArtifacts: List<MavenArtifact>,
                              dockerizedVersions: Set<SemanticVersion>): List<VersionChange> {


        val storedVersions = storedVersionData.map { it.version }.sortedDescending()
        val minimumStoredVersion = storedVersions.last()
        val maxStoredVersionBound = storedVersions.first().major
        return mavenArtifacts
                .mapNotNull { it.version }
                .filter { it.isStable() }
                .filterNot { it < minimumStoredVersion || it.major > maxStoredVersionBound }
                .sorted()
                .groupBy { MajorMinorBranch(it) }
                .flatMap {
                    val matchingStoredVersions = storedVersionData.filter { ex -> MajorMinorBranch(ex.version) == it.key }
                    if (matchingStoredVersions.isNotEmpty()) {
                        computeChanges(matchingStoredVersions, it.value, dockerizedVersions, minimumStoredVersion)
                    } else {
                        computeAdditionsInBranch(it.value, dockerizedVersions)
                    }
                }
                .sorted()
    }

    private fun computeChanges(storedVersionsInBranch: List<StoredVersion>,
                               mavenVersionsInBranch: List<SemanticVersion>,
                               dockerizedVersions: Set<SemanticVersion>,
                               mininumExistingVersion: SemanticVersion): List<VersionChange> {

        val storedMinimumInBranch = storedVersionsInBranch.minBy { it.version }!!
        return if (storedMinimumInBranch.version == mininumExistingVersion) {
            computeChangesInSmallestBranch(storedVersionsInBranch, mavenVersionsInBranch, dockerizedVersions, storedMinimumInBranch)
        } else {
            computeChangesInBranch(mavenVersionsInBranch, dockerizedVersions, storedVersionsInBranch.maxBy { it.version })
        }
    }

    private fun computeChangesInSmallestBranch(storedVersionsInBranch: List<StoredVersion>,
                                               mavenVersionsInBranch: List<SemanticVersion>,
                                               dockerizedVersions: Set<SemanticVersion>,
                                               storedMinimumInBranch: StoredVersion): List<VersionChange> {

        val highestVersionInBranch = storedVersionsInBranch.drop(1).maxBy { it.version }
        val storedMinimumVersion = storedMinimumInBranch.version
        val changes = computeChangesInBranch(mavenVersionsInBranch.filterNot { it == storedMinimumVersion }, dockerizedVersions, highestVersionInBranch)
        return if (!storedMinimumInBranch.inDockerStore && dockerizedVersions.contains(storedMinimumVersion)) {
            listOf(Update(storedMinimumVersion, storedMinimumVersion, dockerized = true)).plus(changes)
        } else {
            changes
        }
    }

    private fun computeChangesInBranch(mavenVersionsInBranch: List<SemanticVersion>,
                                       dockerizedVersions: Set<SemanticVersion>,
                                       highestStoredVersionInBranch: StoredVersion?): List<VersionChange> {

        val highestMavenVersionInBranch = mavenVersionsInBranch.lastOrNull()
        val highestMavenVersionIsDockerized = dockerizedVersions.contains(highestMavenVersionInBranch)
        return when {
            highestMavenVersionInBranch == null -> {
                listOf()
            }
            highestStoredVersionInBranch == null -> {
                listOf(Addition(highestMavenVersionInBranch, highestMavenVersionIsDockerized))
            }
            highestMavenVersionInBranch == highestStoredVersionInBranch.version -> {
                if (highestMavenVersionIsDockerized && !highestStoredVersionInBranch.inDockerStore) {
                    listOf(Update(highestMavenVersionInBranch, highestMavenVersionInBranch, dockerized = true))
                } else {
                    listOf()
                }
            }
            highestMavenVersionInBranch > highestStoredVersionInBranch.version -> {
                computeUpdates(mavenVersionsInBranch, dockerizedVersions, highestStoredVersionInBranch, highestMavenVersionInBranch, highestMavenVersionIsDockerized)
            }
            else -> listOf()
        }
    }

    private fun computeUpdates(mavenVersionsInBranch: List<SemanticVersion>,
                               dockerizedVersions: Set<SemanticVersion>,
                               highestStoredVersionInBranch: StoredVersion,
                               highestMavenVersionInBranch: SemanticVersion,
                               highestMavenVersionIsDockerized: Boolean): List<VersionChange> {

        return if (!highestStoredVersionInBranch.inDockerStore || highestMavenVersionIsDockerized) {
            listOf(Update(highestStoredVersionInBranch.version, highestMavenVersionInBranch, dockerized = highestMavenVersionIsDockerized))
        } else {
            val addition = Addition(highestMavenVersionInBranch, dockerized = false)
            val previousHighestDockerizedVersion = mavenVersionsInBranch.lastOrNull { highestStoredVersionInBranch.version < it && it < highestMavenVersionInBranch && dockerizedVersions.contains(it) }
            if (previousHighestDockerizedVersion == null) {
                listOf(addition)
            } else {
                listOf(addition, Update(highestStoredVersionInBranch.version, previousHighestDockerizedVersion, dockerized = true))
            }
        }
    }

    private fun computeAdditionsInBranch(mavenVersionInBranch: List<SemanticVersion>, dockerizedVersions: Set<SemanticVersion>): List<Addition> {
        val highestMavenVersionInBranch = mavenVersionInBranch.last()
        return if (dockerizedVersions.contains(highestMavenVersionInBranch)) {
            listOf(Addition(highestMavenVersionInBranch, dockerized = true))
        } else {
            val highestDockerized = mavenVersionInBranch.lastOrNull { it < highestMavenVersionInBranch && dockerizedVersions.contains(it) }
            if (highestDockerized == null) {
                listOf(Addition(highestMavenVersionInBranch, dockerized = false))
            } else {
                listOf(
                        Addition(highestMavenVersionInBranch, dockerized = false),
                        Addition(highestDockerized, dockerized = true)
                )
            }
        }
    }
}