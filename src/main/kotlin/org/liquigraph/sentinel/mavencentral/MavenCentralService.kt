package org.liquigraph.sentinel.mavencentral

import org.liquigraph.sentinel.WatchedCoordinates
import org.liquigraph.sentinel.effects.Computation
import org.springframework.stereotype.Service

@Service
class MavenCentralService(private val mavenCentralClient: MavenCentralClient) {

    fun getArtifacts(coordinates: WatchedCoordinates.MavenCoordinates): Computation<List<MavenArtifact>> {
        return mavenCentralClient.fetchMavenCentralResults().map { mavenCentralArtifacts ->
            mavenCentralArtifacts.filter {
                byCoordinates(it, coordinates)
            }
        }
    }

    private fun byCoordinates(mavenCentralArtifact: MavenArtifact, watchedCoordinates: WatchedCoordinates.MavenCoordinates): Boolean {
        return mavenCentralArtifact.groupId == watchedCoordinates.groupId
                && mavenCentralArtifact.artifactId == watchedCoordinates.artifactId
                && mavenCentralArtifact.packaging == watchedCoordinates.packaging
                && mavenCentralArtifact.classifiers.contains(watchedCoordinates.classifier)
    }

}