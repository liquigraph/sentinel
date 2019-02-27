package org.liquigraph.sentinel

import org.liquigraph.sentinel.configuration.WatchedCoordinates
import org.liquigraph.sentinel.dockerstore.DockerStoreService
import org.liquigraph.sentinel.effects.Computation
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
                     private val dockerStoreService: DockerStoreService,
                     private val watchedCoordinates: WatchedCoordinates) : CommandLineRunner {

    @Suppress("UNCHECKED_CAST")
    override fun run(vararg args: String) {
        readFromGithub().consume { buildDefinition ->
            readFromMavenCentral(watchedCoordinates.maven).consume { mavenVersions ->
                readFromDockerStore(watchedCoordinates.docker).consume { dockerVersions ->
                    storedVersionParser.parse(buildDefinition).consume { storedVersions ->

                        val versionChanges = updateService.computeVersionChanges(
                                storedVersions,
                                mavenVersions,
                                dockerVersions
                        )

                        val updatedBuildDefinition = storedVersionService.update(buildDefinition, versionChanges)

                        println("#### Github (showing max 10)")
                        println(storedVersions.take(10).joinLines())
                        println("#### Maven Central (showing max 10)")
                        println(mavenVersions.take(10).joinLines())
                        println("#### Docker Store (showing max 10)")
                        println(dockerVersions.take(10).joinLines())
                        println("#### Changes")
                        println(versionChanges.joinLines())
                        println("#### Resulting Yaml")
                        println(updatedBuildDefinition.getOrThrow())
                    }
                }
            }
        }
    }

    fun readFromGithub(): Computation<String> {
        return storedVersionService.getBuildDefinition()
    }

    fun readFromMavenCentral(mavenCoordinates: WatchedCoordinates.MavenCoordinates): Computation<List<MavenArtifact>> {
        return mavenCentralService.getArtifacts(mavenCoordinates)
    }

    fun readFromDockerStore(dockerImage: WatchedCoordinates.DockerCoordinates): Computation<Set<SemanticVersion>> {
        return dockerStoreService.getVersions(dockerImage)
    }

}
