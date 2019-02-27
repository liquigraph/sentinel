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
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import java.util.logging.LogManager

class DockerStoreServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: DockerStoreService
    private val watchedArtifact = dockerDefinition("neo4j")

    init {
        LogManager.getLogManager().reset()
    }

    @BeforeEach
    fun prepare() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        service = DockerStoreService(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `retrieves the Docker images from Docker Store`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.dockerStubResponse))

        val result = service.getVersions(watchedArtifact)

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

        val error = service.getVersions(watchedArtifact)

        assertThat(error).isEqualTo(Failure<String>(404, "4xx error"))
    }

    @Test
    fun `propagates a 500 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val error = service.getVersions(watchedArtifact)

        assertThat(error).isEqualTo(Failure<String>(500, "Unreachable http://localhost:${mockWebServer.port}"))
    }

    @Test
    fun `propagates a weird error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(666))

        val error = service.getVersions(watchedArtifact)

        assertThat(error).isEqualTo(Failure<String>(666, "Unexpected error"))
    }

    @Test
    fun `returns invalid JSON error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("loliloljson"))

        val result = service.getVersions(watchedArtifact) as Failure

        assertThat(result.code).isEqualTo(3001)
        assertThat(result.message).containsIgnoringCase("Expected BEGIN_OBJECT but was STRING")
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
