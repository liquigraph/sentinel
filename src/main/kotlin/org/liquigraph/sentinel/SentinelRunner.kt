package org.liquigraph.sentinel

import org.liquigraph.sentinel.configuration.WatchedArtifact
import org.liquigraph.sentinel.dockerstore.DockerStoreClient
import org.liquigraph.sentinel.effects.flatMap
import org.liquigraph.sentinel.effects.forEach
import org.liquigraph.sentinel.github.StoredVersionParser
import org.liquigraph.sentinel.github.StoredVersionService
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SentinelRunner(private val storedVersionService: StoredVersionService,
                     private val storedVersionParser: StoredVersionParser,
                     private val mavenCentralService: MavenCentralService,
                     private val updateService: UpdateService,
                     private val dockerStoreClient: DockerStoreClient,
                     private val watchedCoordinates: WatchedArtifact) : CommandLineRunner {

    override fun run(vararg args: String) {
        readFromGithub().forEach { buildDefinition ->
            readFromMavenCentral(watchedCoordinates.maven).forEach { mavenVersions ->
                readFromDockerStore(watchedCoordinates.docker).forEach { dockerVersions ->
                    storedVersionParser.parse(buildDefinition).forEach { storedVersions ->
                        val versionChanges = updateService.computeVersionChanges(storedVersions, mavenVersions, dockerVersions)
                        storedVersionService.applyChanges(buildDefinition, versionChanges)
                                .flatMap { storedVersionService.postPullRequest(it) }
                                .forEach {
                                    println("New PR created: $it")
                                }
                    }
                }
            }
        }
    }

    fun readFromGithub(): Result<String> {
        return storedVersionService.getBuildDefinition()
    }

    fun readFromMavenCentral(mavenCoordinates: WatchedArtifact.MavenCoordinates): Result<List<MavenArtifact>> {
        return mavenCentralService.getArtifacts(mavenCoordinates)
    }

    fun readFromDockerStore(dockerImage: WatchedArtifact.DockerCoordinates): Result<Set<SemanticVersion>> {
        return dockerStoreClient.getVersions(dockerImage)
    }

}
