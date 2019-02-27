package org.liquigraph.sentinel

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.liquigraph.sentinel.dockerstore.DockerStoreService
import org.liquigraph.sentinel.github.StoredVersionParser
import org.liquigraph.sentinel.github.StoredVersionService
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

    override fun run(vararg args: String) = runBlocking {
        val deferredBuildDefinition = async { readFromGithub() }
        val deferredMavenVersions = async { readFromMavenCentral() }
        val deferredDockerVersions = async { readFromDockerStore() }

        val buildDefinition = deferredBuildDefinition.await().invoke()
        val githubVersions = travisYamlParser.parse(buildDefinition).getOrThrow()

        val mavenVersions = deferredMavenVersions.await().invoke()
        val dockerVersions = deferredDockerVersions.await().invoke()
        val versionChanges = updateService.computeVersionChanges(
                githubVersions,
                mavenVersions,
                dockerVersions
        )

        val updatedYaml = storedVersionService.update(buildDefinition, versionChanges)

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

    suspend fun readFromGithub() = suspend {
        storedVersionService.fetchTravisYaml().getOrThrow()
    }

    suspend fun readFromMavenCentral() = suspend {
        mavenCentralService.getNeo4jArtifacts().getOrThrow()
    }

    suspend fun readFromDockerStore() = suspend {
        dockerStoreService.fetchDockerizedNeo4jVersions().getOrThrow()
    }

}
