package org.liquigraph.sentinel.github

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions
import org.junit.Test
import org.liquigraph.sentinel.getContentOrThrow
import org.liquigraph.sentinel.model.Success
import org.yaml.snakeyaml.Yaml
import java.nio.charset.StandardCharsets
import java.util.*

class LiquigraphServiceIntegrationTest {

    @Test
    fun `retrieves the content of a file`() {
        val travisYmlBase64 = Base64.getEncoder().encodeToString("""
            |sudo: required
            |language: java
            |services:
            |  - docker
            |jdk:
            |  - oraclejdk8
            |os:
            |  - linux
            |env:
            |  matrix:
            |    - NEO_VERSION=3.0.11
            |      WITH_DOCKER=true
            |      EXTRA_PROFILES=-Pwith-neo4j-io
            |    - NEO_VERSION=3.1.7
            |      WITH_DOCKER=false
            |      EXTRA_PROFILES=-Pwith-neo4j-io
        """.trimMargin().toByteArray(StandardCharsets.UTF_8))

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""
             {
                "name": ".travis.yml",
                "path": ".travis.yml",
                "sha": "45e87146451aa1f8cae0eb76297d563fc77776ad",
                "size": 1054,
                "url": "https://api.github.com/repos/liquigraph/liquigraph/contents/.travis.yml?ref=master",
                "html_url": "https://github.com/liquigraph/liquigraph/blob/master/.travis.yml",
                "git_url": "https://api.github.com/repos/liquigraph/liquigraph/git/blobs/45e87146451aa1f8cae0eb76297d563fc77776ad",
                "download_url": "https://raw.githubusercontent.com/liquigraph/liquigraph/master/.travis.yml",
                "type": "file",
                "content": "\n$travisYmlBase64\n",
                "encoding": "base64",
                "_links": {
                    "self": "https://api.github.com/repos/liquigraph/liquigraph/contents/.travis.yml?ref=master",
                    "git": "https://api.github.com/repos/liquigraph/liquigraph/git/blobs/45e87146451aa1f8cae0eb76297d563fc77776ad",
                    "html": "https://github.com/liquigraph/liquigraph/blob/master/.travis.yml"
                }
            }
            """.trimIndent()))
        mockWebServer.start()

        val subject = LiquigraphService(
                TravisYamlClient(
                        Gson(),
                        OkHttpClient(),
                        "http://localhost:${mockWebServer.port}"),
                Neo4jVersionParser(Yaml())
        )

        val result = subject.getNeo4jVersions() as Success

        Assertions.assertThat(result.getContentOrThrow()).containsExactly(
                Neo4jVersion("3.0.11", true),
                Neo4jVersion("3.1.7", false)
        )
    }

}