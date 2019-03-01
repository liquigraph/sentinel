package org.liquigraph.sentinel.dockerstore

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.liquigraph.sentinel.*
import org.liquigraph.sentinel.configuration.WatchedArtifact
import java.util.logging.LogManager

class DockerStoreClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: DockerStoreClient
    private val watchedArtifact = dockerDefinition("neo4j")

    init {
        LogManager.getLogManager().reset()
    }

    @BeforeEach
    fun prepare() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = DockerStoreClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `retrieves the Docker images from Docker Store`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.dockerStubResponse))

        val result = client.getVersions(watchedArtifact)

        assertThat(result)
                .isEqualTo(Result.success(setOf(
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

        val error = client.getVersions(watchedArtifact)

        assertThat(error.exceptionOrNull()!!.message)
                .startsWith("Call on http://localhost:${mockWebServer.port}")
                .endsWith("resulted in response code 404")
    }

    @Test
    fun `returns invalid JSON error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("loliloljson"))

        val result = client.getVersions(watchedArtifact)

        assertThat(result.exceptionOrNull()!!.message).contains("Expected BEGIN_OBJECT but was STRING")
    }

    private fun gson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(SemanticVersion::class.java, SemanticVersionAdapter())
                .create()
    }

    private fun dockerDefinition(imageName: String): WatchedArtifact.DockerCoordinates {
        val result = WatchedArtifact.DockerCoordinates()
        result.image = imageName
        return result
    }

}
