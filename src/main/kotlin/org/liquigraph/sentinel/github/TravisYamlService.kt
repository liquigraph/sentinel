package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml
import java.io.StringWriter

@Service
class TravisYamlService(val travisYamlClient: TravisYamlClient,
                        val neo4jVersionParser: TravisNeo4jVersionParser,
                        val yamlParser: Yaml) {

    fun getNeo4jVersions(): Result<List<TravisNeo4jVersion>> {

        return travisYamlClient.fetchTravisYaml().flatMap {
            neo4jVersionParser.parse(it)
        }
    }

    fun update(versionChanges : List<VersionChange>): Result<String> {
        return travisYamlClient.fetchTravisYaml().flatMap {
            val initialVersions = (neo4jVersionParser.parse(it) as Success<List<TravisNeo4jVersion>>).content.toMutableList()
            val content = yamlParser.load<Map<String, Any>>(it)

            initialVersions.addAll(versionChanges.filterIsInstance(Addition::class.java) //
                                                 .map { TravisNeo4jVersion(it.new, it.dockerized) } )

            val matrix = initialVersions.sortedBy { it.version } //
                                        .map { "NEO_VERSION=${it.version} WITH_DOCKER=${it.inDockerStore}" } //
                                        .toList()

            val initialMatrix = (content["env"] as Map<String, MutableList<String>>)["matrix"]!!
            initialMatrix.clear()
            initialMatrix.addAll(matrix)

            val resultContent = StringWriter()
            yamlParser.dump( content, resultContent )

            Success( resultContent.toString() )
        }
    }
}

