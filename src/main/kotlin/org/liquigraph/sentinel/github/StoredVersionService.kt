package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.Addition
import org.liquigraph.sentinel.Update
import org.liquigraph.sentinel.VersionChange
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml
import java.io.StringWriter

@Service
class StoredVersionService(private val storedBuildClient: StoredBuildClient,
                           private val neo4jVersionParser: StoredVersionParser,
                           private val yamlParser: Yaml) {

    fun update(rawBuildDefinition: String,
               versionChanges: List<VersionChange>): Result<String> {

        val initialVersions = neo4jVersionParser.parse(rawBuildDefinition)
        return when (initialVersions) {
            is Failure<List<StoredVersion>> -> Failure(4001, "Could not parse .travis.yml")
            is Success<List<StoredVersion>> -> {
                val updated = applyUpdates(versionChanges, initialVersions)
                val completeVersions = applyAdditions(versionChanges, updated)
                return serializeYaml(rawBuildDefinition, completeVersions)
            }
        }
    }

    private fun serializeYaml(rawTravisYaml: String, completeVersions: List<StoredVersion>): Success<String> {
        StringWriter().use {
            val content = updateVersionMatrix(rawTravisYaml, completeVersions)
            yamlParser.dump(content, it)
            return Success(it.toString())
        }
    }

    private fun serialize(completeVersions: List<StoredVersion>): List<String> {
        return completeVersions
                .sortedBy { it.version }
                .map { "NEO_VERSION=${it.version} WITH_DOCKER=${it.inDockerStore}" }
                .toList()
    }

    private fun updateVersionMatrix(rawTravisYaml: String, completeVersions: List<StoredVersion>): Map<String, Any>? {
        val content = yamlParser.load<Map<String, Any>>(rawTravisYaml)
        val initialMatrix = (content["env"] as Map<String, MutableList<String>>)["matrix"]!!
        initialMatrix.clear()
        initialMatrix.addAll(serialize(completeVersions))
        return content
    }

    private fun applyAdditions(versionChanges: List<VersionChange>, updatedVersions: List<StoredVersion>): List<StoredVersion> {
        return updatedVersions + versionChanges.filterIsInstance(Addition::class.java)
                .map { StoredVersion(it.new, it.dockerized) }
    }

    private fun applyUpdates(versionChanges: List<VersionChange>, initialVersions: Success<List<StoredVersion>>): List<StoredVersion> {
        val updates = versionChanges.filterIsInstance(Update::class.java)
        val updatedVersions = initialVersions.content.map { version ->
            val update = updates.firstOrNull { update -> version.version == update.old }
            if (update == null) version
            else {
                StoredVersion(update.new, update.dockerized)
            }
        }
        return updatedVersions
    }

    fun fetchTravisYaml(): Result<String> {
        return storedBuildClient.fetchBuildDefinition()
    }
}

