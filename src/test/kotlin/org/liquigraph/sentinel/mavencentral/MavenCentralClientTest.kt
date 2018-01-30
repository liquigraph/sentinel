package org.liquigraph.sentinel.mavencentral

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.github.SemanticVersion
import org.liquigraph.sentinel.github.SemanticVersionAdapter
import java.util.logging.LogManager

class MavenCentralClientTest {

    init {
        LogManager.getLogManager().reset()
    }

    @Test
    fun `retrieves the Neo4j Versions from Maven Central`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.mavenCentralApiResponse))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults()

        assertThat(result).isEqualTo(Success(Fixtures.mavenCentralArtifacts))
    }

    @Test
    fun `returns an error with 404`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults()

        assertThat(result).isEqualTo(Failure<List<MavenArtifact>>(404, "4xx error"))
    }

    @Test
    fun `returns an error with 500`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults()

        assertThat(result).isEqualTo(Failure<List<MavenArtifact>>(500, "Unreachable http://localhost:${mockWebServer.port}"))
    }

    @Test
    fun `returns an unknown error`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(758))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults()

        assertThat(result).isEqualTo(Failure<List<MavenArtifact>>(758, "Unexpected error"))
    }

    @Test
    fun `returns invalid JSON error`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("loliloljson"))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults() as Failure

        assertThat(result.code).isEqualTo(2001)
        assertThat(result.message).containsIgnoringCase("Expected BEGIN_OBJECT but was STRING")
    }

    @Test
    fun `returns missing 'response' field error`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults() as Failure

        assertThat(result.code).isEqualTo(2002)
        assertThat(result.message).containsIgnoringCase("Could not find 'response' field")
    }

    @Test
    fun `returns missing 'docs' field error`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"response\": {}}"))
        mockWebServer.start()
        val subject = MavenCentralClient(OkHttpClient(), gson(), "http://localhost:${mockWebServer.port}")

        val result = subject.fetchMavenCentralResults() as Failure

        assertThat(result.code).isEqualTo(2002)
        assertThat(result.message).containsIgnoringCase("Could not find 'docs' field")
    }


    private fun gson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(SemanticVersion::class.java, SemanticVersionAdapter())
                .create()
    }
}

