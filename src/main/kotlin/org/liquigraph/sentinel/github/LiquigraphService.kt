package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.model.Result
import org.liquigraph.sentinel.model.Success
import org.liquigraph.sentinel.model.Error

class LiquigraphService(val travisYamlClient: TravisYamlClient,
                        val neo4jVersionParser: Neo4jVersionParser) {

    fun getNeo4jVersions(): Result<List<Neo4jVersion>> {

        val result = travisYamlClient.fetchTravisYaml()

        return when (result) {
            is Error<String> -> Error(result.code, result.message)
            is Success<String> -> Success(neo4jVersionParser.parse((result).getContent()))
        }
    }
}

data class Neo4jVersion(val version: String, val inDockerStore: Boolean)