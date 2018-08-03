package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml
import java.io.StringWriter

@Service
class TravisYamlService(private val travisYamlClient: TravisYamlClient,
                        private val neo4jVersionParser: TravisNeo4jVersionParser,
                        private val yamlParser: Yaml) {

    fun update(rawTravisYaml: String,
               versionChanges: List<VersionChange>): Result<String> {

        val initialVersions = neo4jVersionParser.parse(rawTravisYaml)
        return when (initialVersions) {
            is Failure<List<TravisNeo4jVersion>> -> Failure(4001, "Could not parse .travis.yml")
            is Success<List<TravisNeo4jVersion>> -> {
                val updated = applyUpdates(versionChanges, initialVersions)
                val completeVersions = applyAdditions(versionChanges, updated)
                return serializeYaml(rawTravisYaml, completeVersions)
            }
        }
    }

    private fun serializeYaml(rawTravisYaml: String, completeVersions: List<TravisNeo4jVersion>): Success<String> {
        StringWriter().use {
            val content = updateVersionMatrix(rawTravisYaml, completeVersions)
            yamlParser.dump(content, it)
            return Success(it.toString())
        }
    }

    private fun serialize(completeVersions: List<TravisNeo4jVersion>): List<String> {
        return completeVersions
                .sortedBy { it.version }
                .map { "NEO_VERSION=${it.version} WITH_DOCKER=${it.inDockerStore}" }
                .toList()
    }

    private fun updateVersionMatrix(rawTravisYaml: String, completeVersions: List<TravisNeo4jVersion>): Map<String, Any>? {
        val content = yamlParser.load<Map<String, Any>>(rawTravisYaml)
        val initialMatrix = (content["env"] as Map<String, MutableList<String>>)["matrix"]!!
        initialMatrix.clear()
        initialMatrix.addAll(serialize(completeVersions))
        return content
    }

    private fun applyAdditions(versionChanges: List<VersionChange>, updatedVersions: List<TravisNeo4jVersion>): List<TravisNeo4jVersion> {
        return updatedVersions + versionChanges.filterIsInstance(Addition::class.java)
                .map { TravisNeo4jVersion(it.new, it.dockerized) }
    }

    private fun applyUpdates(versionChanges: List<VersionChange>, initialVersions: Success<List<TravisNeo4jVersion>>): List<TravisNeo4jVersion> {
        val updates = versionChanges.filterIsInstance(Update::class.java)
        val updatedVersions = initialVersions.content.map { version ->
            val update = updates.firstOrNull { update -> version.version == update.old }
            if (update == null) version
            else {
                TravisNeo4jVersion(update.new, update.dockerized)
            }
        }
        return updatedVersions
    }

    fun fetchTravisYaml(): Result<String> {
        return travisYamlClient.fetchTravisYaml()
    }
}

