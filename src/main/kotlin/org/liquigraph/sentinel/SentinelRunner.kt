package org.liquigraph.sentinel

import org.liquigraph.sentinel.github.LiquigraphService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SentinelRunner(private val liquigraphService: LiquigraphService): CommandLineRunner {

    override fun run(vararg p0: String?) {
        val neo4jVersions = liquigraphService.getNeo4jVersions()
        if (!neo4jVersions.isSuccessful()) {
            System.err.println(neo4jVersions)
            return
        }
        println(neo4jVersions.getContent())
    }

}