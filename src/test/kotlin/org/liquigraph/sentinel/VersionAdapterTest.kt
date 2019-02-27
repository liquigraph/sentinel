package org.liquigraph.sentinel

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.mavencentral.MavenCentralResponse
import org.liquigraph.sentinel.mavencentral.MavenCentralResult

class SemanticVersionAdapterTest {

    private val gson = subject(SemanticVersionAdapter())

    @Test
    fun `converts JSON with version`() {
        val json = mavenCentralResult("3.4.0-alpha04")

        val result = gson.fromJson(json, MavenCentralResult::class.java)

        val expectedArtifact = MavenArtifact("org.neo4j", "neo4j", "3.4.0-alpha04".toVersion(), "jar", listOf(".pom"))
        assertThat(result).isEqualTo(MavenCentralResult(MavenCentralResponse(listOf(expectedArtifact))))
    }

    private fun subject(adapter: SemanticVersionAdapter): Gson {
        return GsonBuilder()
                .registerTypeAdapter(SemanticVersion::class.java, adapter)
                .create()
    }

    private fun mavenCentralResult(version: String): String {
        return """
            {
               "response":{
                  "docs":[
                     {
                        "g":"org.neo4j",
                        "a":"neo4j",
                        "v":"$version",
                        "p":"jar",
                        "ec":[
                           ".pom"
                        ]
                     }
                  ]
               }
    }""".trimIndent()
    }
}