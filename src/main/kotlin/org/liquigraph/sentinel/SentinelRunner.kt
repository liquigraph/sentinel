package org.liquigraph.sentinel

import org.liquigraph.sentinel.github.TravisYamlService
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.github.LiquigraphService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SentinelRunner(private val travisYamlService: TravisYamlService,
                     private val mavenCentralServices: MavenCentralService,
                     private val liquigraphService: LiquigraphService): CommandLineRunner {

    override fun run(vararg args: String?) {
        val testedNeo4jVersions = travisYamlService.getNeo4jVersions()
        when (testedNeo4jVersions) {
            is Success -> {
                println("Fetched from Github")
                println(testedNeo4jVersions.content)
                val mavenCentralNeo4jVersions = mavenCentralServices.getNeo4jArtifacts()
                when (mavenCentralNeo4jVersions) {
                    is Success -> {
                        println("Fetched from Maven Central")
                        println(mavenCentralNeo4jVersions.content)
                        println("Changing versions")
                        println(liquigraphService.retainNewVersions(testedNeo4jVersions.content, mavenCentralNeo4jVersions.content))

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