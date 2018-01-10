package org.liquigraph.sentinel.mavencentral

import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.springframework.stereotype.Service

@Service
class MavenCentralService(private val mavenCentralClient: MavenCentralClient) {

    private val orgNeo4j = "org.neo4j"
    private val neo4j = "neo4j"
    private val jar = "jar"
    private val classifier = ".jar"

    fun getNeo4jArtifacts(): Result<List<MavenArtifact>> {
        val result = mavenCentralClient.fetchMavenCentralResults()

        return when (result) {
            is Failure -> Failure(result.code, result.message)
            is Success -> Success(result.filterCoordinates(orgNeo4j, neo4j, jar, classifier))
        }
    }

    private fun Success<List<MavenArtifact>>.filterCoordinates(groupId: String,
                                                               artifactId: String,
                                                               packaging: String,
                                                               classifier: String): List<MavenArtifact> =
            content.filter {
                it.groupId == groupId
                        && it.artifactId == artifactId
                        && it.packaging == packaging
                        && it.classifiers.contains(classifier)
            }

}