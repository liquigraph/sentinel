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
            val content = yamlParser.load<Map<String, Any>>(it)
            val matrix = (content["env"] as Map<String, Any>)["matrix"] as MutableList<String>

            versionChanges.filterIsInstance(Addition::class.java)
                    .map { "NEO_VERSION=${it.newVersion()} WITH_DOCKER=${it.dockerized}" }
                    .forEach({ matrix.add(it) })

            //matrix.sortWith(Comparator { o1, o2 -> SemanticVersion.parseEntire(o1)!!.compareTo(SemanticVersion.parseEntire(o2)!!) })

            val writer = StringWriter()
            yamlParser.dump(content, writer)

            Success(writer.toString())
        }
    }
}

