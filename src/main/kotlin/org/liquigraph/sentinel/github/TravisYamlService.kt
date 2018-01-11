package org.liquigraph.sentinel.github

import org.springframework.stereotype.Service
import org.liquigraph.sentinel.effects.Result

@Service
class TravisYamlService(val travisYamlClient: TravisYamlClient,
                        val neo4jVersionParser: TravisNeo4jVersionParser) {

    fun getNeo4jVersions(): Result<List<TravisNeo4jVersion>> {

        return travisYamlClient.fetchTravisYaml().flatMap {
            neo4jVersionParser.parse(it)
        }
    }
}

