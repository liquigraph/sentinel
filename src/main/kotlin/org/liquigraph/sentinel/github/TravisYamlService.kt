package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.effects.Failure
import org.springframework.stereotype.Service

@Service
class TravisYamlService(val travisYamlClient: TravisYamlClient,
                        val neo4jVersionParser: TravisNeo4jVersionParser) {

    fun getNeo4jVersions(): Result<List<TravisNeo4jVersion>> {

        val result = travisYamlClient.fetchTravisYaml()

        return when (result) {
            is Failure -> Failure(result.code, result.message)
            is Success -> neo4jVersionParser.parse(result.content)
        }
    }
}

