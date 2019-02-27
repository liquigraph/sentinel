package org.liquigraph.sentinel.mavencentral

import org.liquigraph.sentinel.effects.Result
import org.springframework.stereotype.Service

@Service
class MavenCentralService(private val mavenCentralClient: MavenCentralClient) {

    private val targetOrg = "org.neo4j"
    private val targetArtifactId = "neo4j"
    private val targetPackaging = "jar"
    private val targetClassifier = ".jar"

    fun getNeo4jArtifacts(): Result<List<MavenArtifact>> {
        return mavenCentralClient.fetchMavenCentralResults().map {
            it.filter {
                it.groupId == targetOrg
                        && it.artifactId == targetArtifactId
                        && it.packaging == targetPackaging
                        && it.classifiers.contains(targetClassifier)
            }
        }
    }

}