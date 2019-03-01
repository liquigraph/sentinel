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
import java.util.logging.LogManager

class StoredBuildClientTest {

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

        assertThat(yaml).isEqualTo(Result.success(Fixtures.travisYml))
    }

    @Test
    fun `propagates a 404 error`() {
        val expectedErrorMessage = "Call on http://localhost:${mockWebServer.port}/repos/liquigraph/liquigraph/contents/.travis.yml resulted in response code 404"
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{
    "message": "Not Found",
    "documentation_url": "https://developer.github.com/v3"
}""".trimIndent()))

        val error = client.fetchBuildDefinition()

        assertThat(error.exceptionOrNull()!!.message).isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `returns invalid JSON error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("loliloljson"))

        val result = client.fetchBuildDefinition()

        assertThat(result.exceptionOrNull()!!.message).contains("Expected BEGIN_OBJECT but was STRING")
    }

    private fun githubRepository(org: String, repo: String, branch: String): WatchedGithubRepository {
        val result = WatchedGithubRepository()
        result.organization = org
        result.repository = repo
        result.branch = branch
        return result
    }
}

