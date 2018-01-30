package org.liquigraph.sentinel.dockerstore

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.bouncycastle.crypto.tls.ConnectionEnd.client
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.github.SemanticVersion
import org.liquigraph.sentinel.github.SemanticVersionAdapter
import org.liquigraph.sentinel.mavencentral.MavenCentralClient
import org.liquigraph.sentinel.toVersion
import java.util.logging.LogManager

class DockerStoreClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: DockerStoreClient

    init {
        LogManager.getLogManager().reset()
    }

    @Before
    fun prepare() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = DockerStoreClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `retrieves the Docker images from Docker Store`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.dockerStubResponse))

        val result = client.fetchDockerStoreResults()

        assertThat(result)
                .isEqualTo(Success(setOf(
                        "3.3.1".toVersion(),
                        "3.3.0".toVersion(),
                        "3.3.2".toVersion())))
    }

    @Test
    fun `propagates a 404 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{
    "message": "Not Found",
    "documentation_url": "https://developer.github.com/v3"
}""".trimIndent()))

        val error = client.fetchDockerStoreResults()

        assertThat(error).isEqualTo(Failure<String>(404, "4xx error"))
    }

    @Test
    fun `propagates a 500 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val error = client.fetchDockerStoreResults()

        assertThat(error).isEqualTo(Failure<String>(500, "Unreachable http://localhost:${mockWebServer.port}"))
    }

    @Test
    fun `propagates a weird error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(666))

        val error = client.fetchDockerStoreResults()

        assertThat(error).isEqualTo(Failure<String>(666, "Unexpected error"))
    }

    @Test
    fun `returns invalid JSON error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("loliloljson"))

        val result = client.fetchDockerStoreResults() as Failure

        assertThat(result.code).isEqualTo(3001)
        assertThat(result.message).containsIgnoringCase("Expected BEGIN_OBJECT but was STRING")
    }

    private fun gson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(SemanticVersion::class.java, SemanticVersionAdapter())
                .create()
    }

}