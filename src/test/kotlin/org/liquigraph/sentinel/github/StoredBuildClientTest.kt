package org.liquigraph.sentinel.github

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.configuration.WatchedGithubRepository
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import java.util.logging.LogManager

class StoredBuildClientTest {

    init {
        LogManager.getLogManager().reset();
    }

    lateinit var mockWebServer: MockWebServer

    lateinit var client: StoredBuildClient

    val repository = githubRepository("liquigraph", "liquigraph", "master")

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = StoredBuildClient(Gson(), OkHttpClient(), repository, "http://localhost:${mockWebServer.port}")
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.close()
    }

    @Test
    fun `fetches Travis build file`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Fixtures.githubFileApiResponse))

        val yaml = client.fetchBuildDefinition()

        assertThat(yaml).isEqualTo(Success(Fixtures.travisYml))
    }

    @Test
    fun `propagates a 404 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{
    "message": "Not Found",
    "documentation_url": "https://developer.github.com/v3"
}""".trimIndent()))

        val error = client.fetchBuildDefinition()

        assertThat(error).isEqualTo(Failure<String>(404, "Not Found"))
    }

    @Test
    fun `propagates a 500 error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val error = client.fetchBuildDefinition()

        assertThat(error).isEqualTo(Failure<String>(500, "Unreachable http://localhost:${mockWebServer.port}"))
    }

    @Test
    fun `propagates a weird error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(666))

        val error = client.fetchBuildDefinition()

        assertThat(error).isEqualTo(Failure<String>(666, "Unexpected error"))
    }

    @Test
    fun `returns invalid JSON error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("loliloljson"))

        val result = client.fetchBuildDefinition() as Failure

        assertThat(result.code).isEqualTo(1001)
        assertThat(result.message).containsIgnoringCase("Expected BEGIN_OBJECT but was STRING")
    }

    private fun githubRepository(org: String, repo: String, branch: String): WatchedGithubRepository {
        val result = WatchedGithubRepository()
        result.organization = org
        result.repository = repo
        result.branch = branch
        return result
    }
}

