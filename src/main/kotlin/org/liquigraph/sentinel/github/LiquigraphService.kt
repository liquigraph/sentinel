package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.model.Result
import org.liquigraph.sentinel.model.Success
import org.liquigraph.sentinel.model.Failure
import org.springframework.stereotype.Service

@Service
class LiquigraphService(val travisYamlClient: TravisYamlClient,
                        val neo4jVersionParser: Neo4jVersionParser) {

    fun getNeo4jVersions(): Result<List<Neo4jVersion>> {

        val result = travisYamlClient.fetchTravisYaml()

        return when (result) {
            is Failure<String> -> Failure(result.code, result.message)
            is Success<String> -> neo4jVersionParser.parse((result).getContent())
        }
    }
}

data class Neo4jVersion(val version: String, val inDockerStore: Boolean)