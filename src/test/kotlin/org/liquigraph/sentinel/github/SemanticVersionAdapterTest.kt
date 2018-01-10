package org.liquigraph.sentinel.github

import com.google.gson.GsonBuilder
import org.assertj.core.api.Assertions
import org.junit.Test
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.mavencentral.MavenCentralResponse
import org.liquigraph.sentinel.mavencentral.MavenCentralResult
import org.liquigraph.sentinel.toVersion

class SemanticVersionAdapterTest {

    @Test
    fun `converts JSON with version`() {
        val str = """
        {
           "response":{
              "docs":[
                 {
                    "g":"org.neo4j",
                    "a":"neo4j",
                    "v":"3.4.0-alpha04",
                    "p":"jar",
                    "ec":[
                       ".pom"
                    ]
                 }
              ]
           }
}""".trimIndent()

        val gson = GsonBuilder()
                .registerTypeAdapter(SemanticVersion::class.java, SemanticVersionAdapter())
                .create()

        val result = gson.fromJson(str, MavenCentralResult::class.java)

        Assertions.assertThat(result).isEqualTo(
                MavenCentralResult(
                        MavenCentralResponse(
                                listOf(
                                        MavenArtifact(
                                                "org.neo4j",
                                                "neo4j",
                                                "3.4.0-alpha04".toVersion(),
                                                "jar",
                                                listOf(".pom")
                                        )
                                )
                        )
                )
        )
    }
}