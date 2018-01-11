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
                println("#### Fetched from Github")
                println(testedNeo4jVersions.content.joinLines())
                val mavenCentralNeo4jVersions = mavenCentralServices.getNeo4jArtifacts()
                when (mavenCentralNeo4jVersions) {
                    is Success -> {
                        println("#### Fetched from Maven Central")
                        println(mavenCentralNeo4jVersions.content.joinLines())
                        println("#### Changing versions")
                        val versionChanges = liquigraphService.retainNewVersions(testedNeo4jVersions.content, mavenCentralNeo4jVersions.content)
                        println(versionChanges.joinLines())

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

fun <T> List<T>.joinLines(): String = joinLines { it.toString() }
fun <T> List<T>.joinLines(f: (T) -> String): String = joinToString("\n") { f(it) }