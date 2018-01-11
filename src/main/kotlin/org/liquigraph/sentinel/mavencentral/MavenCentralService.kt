package org.liquigraph.sentinel.mavencentral

import org.liquigraph.sentinel.effects.Result
import org.springframework.stereotype.Service

@Service
class MavenCentralService(private val mavenCentralClient: MavenCentralClient) {

    private val orgNeo4j = "org.neo4j"
    private val neo4j = "neo4j"
    private val jar = "jar"
    private val classifier = ".jar"

    fun getNeo4jArtifacts(): Result<List<MavenArtifact>> {
        return mavenCentralClient.fetchMavenCentralResults().map {
            it.filter {
                it.groupId == orgNeo4j
                        && it.artifactId == neo4j
                        && it.packaging == jar
                        && it.classifiers.contains(classifier)
            }
        }
    }

}