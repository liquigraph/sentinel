package org.liquigraph.sentinel.github

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.model.Failure
import org.liquigraph.sentinel.model.Success

class TravisYamlClientTest {

    lateinit var mockWebServer: MockWebServer

    lateinit var client: TravisYamlClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = TravisYamlClient(Gson(), OkHttpClient(), "http://localhost:${mockWebServer.port}")
    }

    @After
    fun tearDown() {
        mockWebServer.close()
    }

    @Test
    fun `fetches Travis build file`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.githubFileApiResponse))

        val yaml = client.fetchTravisYaml()

        assertThat(yaml).isEqualTo(Success(Fixtures.travisYml))
    }

    @Test
    fun `propagates a 404 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{
    "message": "Not Found",
    "documentation_url": "https://developer.github.com/v3"
}""".trimIndent()))

        val error = client.fetchTravisYaml()

        assertThat(error).isEqualTo(Failure<String>(404, "Not Found"))
    }

    @Test
    fun `propagates a 500 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val error = client.fetchTravisYaml()

        assertThat(error).isEqualTo(Failure<String>(500, "Unreachable http://localhost:${mockWebServer.port}"))
    }

    @Test
    fun `propagates a weird error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(666))

        val error = client.fetchTravisYaml()

        assertThat(error).isEqualTo(Failure<String>(666, "Unexpected error"))
    }
}

