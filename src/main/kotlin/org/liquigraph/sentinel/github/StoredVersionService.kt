package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.Addition
import org.liquigraph.sentinel.Update
import org.liquigraph.sentinel.VersionChange
import org.liquigraph.sentinel.configuration.BotPullRequestSettings
import org.liquigraph.sentinel.effects.Computation
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml
import java.io.StringWriter

@Service
class StoredVersionService(private val storedBuildClient: StoredBuildClient,
                           private val neo4jVersionParser: StoredVersionParser,
                           private val botPullRequestSettings: BotPullRequestSettings,
                           private val yamlParser: Yaml) {

    fun applyChanges(initialBuildDefinition: String,
                     versionChanges: List<VersionChange>): Computation<String> {

        return neo4jVersionParser.parse(initialBuildDefinition).flatMap {
            val updated = applyUpdates(versionChanges, it)
            val completeVersions = applyAdditions(versionChanges, updated)
            serializeYaml(initialBuildDefinition, completeVersions)
        }
    }

    fun postPullRequest(buildDefinition: String): Computation<String> {
        return Failure(2002, "") /*storedBuildClient.postTravisYamlBlob(buildDefinition)
                .flatMap { blob -> Pair(storedBuildClient.getMostRecentCommitHash(), blob).mapFirst() }
                .flatMap { hashAndBlob ->
                    val baseHash = hashAndBlob.first
                    val treeHash = storedBuildClient.postNewTree(baseHash, hashAndBlob.second)
                    Pair(treeHash, baseHash).mapFirst()
                }
                .flatMap { storedBuildClient.postNewCommit(it.first, it.second, botPullRequestSettings.message) }
                .flatMap { storedBuildClient.postNewRef(botPullRequestSettings.branchName, it) }
                .flatMap { storedBuildClient.postNewPullRequest(it, botPullRequestSettings) }*/
    }

    private fun serializeYaml(rawTravisYaml: String, completeVersions: List<StoredVersion>): Success<String> {
        StringWriter().use {
            val content = updateVersionMatrix(rawTravisYaml, completeVersions)
            yamlParser.dump(content, it)
            return Success(it.toString())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateVersionMatrix(rawTravisYaml: String, completeVersions: List<StoredVersion>): Map<String, Any>? {
        val content = yamlParser.load<Map<String, Any>>(rawTravisYaml)
        val initialMatrix = (content["env"] as Map<String, MutableList<String>>)["matrix"]!!
        initialMatrix.clear()
        initialMatrix.addAll(serialize(completeVersions))
        return content
    }

    private fun serialize(completeVersions: List<StoredVersion>): List<String> {
        return completeVersions
                .sortedBy { it.version }
                .map { "NEO_VERSION=${it.version} WITH_DOCKER=${it.inDockerStore}" }
                .toList()
    }

    private fun applyAdditions(versionChanges: List<VersionChange>, updatedVersions: List<StoredVersion>): List<StoredVersion> {
        return updatedVersions + versionChanges.filterIsInstance(Addition::class.java)
                .map { StoredVersion(it.new, it.dockerized) }
    }

    private fun applyUpdates(versionChanges: List<VersionChange>, initialVersions: List<StoredVersion>): List<StoredVersion> {
        val updates = versionChanges.filterIsInstance(Update::class.java)
        return initialVersions.map { version ->
            val update = updates.firstOrNull { update -> version.version == update.old }
            if (update == null) version
            else {
                StoredVersion(update.new, update.dockerized)
            }
        }
    }

    fun getBuildDefinition(): Computation<String> {
        return Failure(2002, "") //storedBuildClient.fetchBuildDefinition()
    }
}

fun <A, B> Pair<Computation<A>, B>.mapFirst(): Computation<Pair<A, B>> {
    return this.first.map { Pair(it, this.second) }
}

fun <A, B> Pair<A, Computation<B>>.mapSecond(): Computation<Pair<A, B>> {
    return this.second.map { Pair(this.first, it) }
}

