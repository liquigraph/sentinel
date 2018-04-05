package org.liquigraph.sentinel

import org.liquigraph.sentinel.dockerstore.DockerStoreService
import org.liquigraph.sentinel.github.TravisYamlService
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.github.LiquigraphService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SentinelRunner(private val travisYamlService: TravisYamlService,
                     private val mavenCentralService: MavenCentralService,
                     private val liquigraphService: LiquigraphService,
                     private val dockerStoreService: DockerStoreService): CommandLineRunner {

    override fun run(vararg args: String?) {
        val testedNeo4jVersions = travisYamlService.getNeo4jVersions()
        when (testedNeo4jVersions) {
            is Success -> {
                println("#### Fetched from Github")
                println(testedNeo4jVersions.content.joinLines())
                val mavenCentralNeo4jVersions = mavenCentralService.getNeo4jArtifacts()
                when (mavenCentralNeo4jVersions) {
                    is Success -> {
                        println("#### Fetched from Maven Central (showing first 10 elements for brevity)")
                        val mavenCentralVersions = mavenCentralNeo4jVersions.content
                        println(mavenCentralVersions.take(10).joinLines())
                        val dockerizedNeo4jVersions = dockerStoreService.fetchDockerizedNeo4jVersions()
                        when (dockerizedNeo4jVersions) {
                            is Success -> {
                                println("#### Fetched from Docker Store (showing first 10 elements for brevity)")
                                val dockerizedVersions = dockerizedNeo4jVersions.content
                                println(dockerizedVersions.take(10).joinLines())
                                println("#### Changing versions")
                                val versionChanges = liquigraphService.computeChanges(
                                        testedNeo4jVersions.content,
                                        mavenCentralVersions,
                                        dockerizedVersions
                                )
                                println(versionChanges.joinLines())
                            }
                            is Failure -> {
                                System.err.println("Failed to fetch from Docker Store")
                                System.err.println(dockerizedNeo4jVersions)
                            }
                        }
                    }
                    is Failure -> {
                        System.err.println("Failed to fetch from Maven Central")
                        System.err.println(mavenCentralNeo4jVersions)
                    }
                }
            }
            else -> {
                System.err.println("Failed to fetch from Github")
                System.err.println(testedNeo4jVersions)
            }
        }
    }
}