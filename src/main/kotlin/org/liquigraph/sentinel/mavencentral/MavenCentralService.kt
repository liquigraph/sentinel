package org.liquigraph.sentinel.mavencentral

import org.liquigraph.sentinel.model.Failure
import org.liquigraph.sentinel.model.MavenCentralArtifact
import org.liquigraph.sentinel.model.Result
import org.liquigraph.sentinel.model.Success
import org.springframework.stereotype.Service

@Service
class MavenCentralService(private val mavenCentralClient: MavenCentralClient) {

    private val orgNeo4j = "org.neo4j"
    private val neo4j = "neo4j"
    private val jar = "jar"
    private val classifier = ".jar"

    fun getNeo4jArtifacts(): Result<List<MavenCentralArtifact>> {
        val result = mavenCentralClient.fetchMavenCentralResults()

        return when (result) {
            is Failure -> Failure(result.code, result.message)
            is Success -> Success(result.filter())
        }
    }

    private fun Success<List<MavenCentralArtifact>>.filter() =
            content.filter {
                        it.groupId == orgNeo4j
                                && it.artifactId == neo4j
                                && it.packaging == jar
                                && it.classifiers.contains(classifier)
                    }

}