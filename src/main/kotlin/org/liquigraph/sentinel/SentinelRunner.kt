package org.liquigraph.sentinel

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.runBlocking
import org.liquigraph.sentinel.dockerstore.DockerStoreService
import org.liquigraph.sentinel.github.LiquigraphService
import org.liquigraph.sentinel.github.SemanticVersion
import org.liquigraph.sentinel.github.TravisNeo4jVersionParser
import org.liquigraph.sentinel.github.TravisYamlService
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SentinelRunner(private val travisYamlService: TravisYamlService,
                     private val travisYamlParser: TravisNeo4jVersionParser,
                     private val mavenCentralService: MavenCentralService,
                     private val liquigraphService: LiquigraphService,
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

            val versionChanges = liquigraphService.computeChanges(
                    githubVersions,
                    mavenVersions,
                    dockerVersions
            )

            val updatedYaml = travisYamlService.update(rawTravisYaml, versionChanges)

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
        travisYamlService.fetchTravisYaml().getOrThrow()
    }

    suspend fun readFromMavenCentral() = async {
        mavenCentralService.getNeo4jArtifacts().getOrThrow()
    }

    suspend fun readFromDockerStore() = async {
        dockerStoreService.fetchDockerizedNeo4jVersions().getOrThrow()
    }
}