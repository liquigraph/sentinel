package org.liquigraph.sentinel

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.runBlocking
import org.liquigraph.sentinel.dockerstore.DockerStoreService
import org.liquigraph.sentinel.github.StoredVersionParser
import org.liquigraph.sentinel.github.StoredVersionService
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SentinelRunner(private val storedVersionService: StoredVersionService,
                     private val travisYamlParser: StoredVersionParser,
                     private val mavenCentralService: MavenCentralService,
                     private val updateService: UpdateService,
                     private val dockerStoreService: DockerStoreService) : CommandLineRunner {

    @Suppress("UNCHECKED_CAST")
    override fun run(vararg args: String) {
        runBlocking {
            val results = awaitAll(
                    readFromGithub(),
                    readFromMavenCentral(),
                    readFromDockerStore()
            )
            val rawTravisYaml = results[0] as String
            val mavenVersions = results[1] as List<MavenArtifact>
            val dockerVersions = results[2] as Set<SemanticVersion>
            val githubVersions = travisYamlParser.parse(rawTravisYaml).getOrThrow()

            val versionChanges = updateService.computeVersionChanges(
                    githubVersions,
                    mavenVersions,
                    dockerVersions
            )

            val updatedYaml = storedVersionService.update(rawTravisYaml, versionChanges)

            println("#### Github (showing max 10)")
            println(githubVersions.take(10).joinLines())
            println("#### Maven Central (showing max 10)")
            println(mavenVersions.take(10).joinLines())
            println("#### Docker Store (showing max 10)")
            println(dockerVersions.take(10).joinLines())
            println("#### Changes")
            println(versionChanges.joinLines())
            println("#### Resulting Yaml")
            println(updatedYaml.getOrThrow())

        }

    }

    suspend fun readFromGithub() = async {
        storedVersionService.fetchTravisYaml().getOrThrow()
    }

    suspend fun readFromMavenCentral() = async {
        mavenCentralService.getNeo4jArtifacts().getOrThrow()
    }

    suspend fun readFromDockerStore() = async {
        dockerStoreService.fetchDockerizedNeo4jVersions().getOrThrow()
    }
}