package org.liquigraph.sentinel.github

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.LogManager

class StoredVersionServiceIntegrationTest {

    init {
        LogManager.getLogManager().reset()
    }

    @Test
    fun `retrieves the content of a file`() {
        val travisYmlBase64 = Base64.getEncoder().encodeToString(Fixtures.travisYml.toByteArray(StandardCharsets.UTF_8))

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

        val subject = StoredVersionService(
                StoredBuildClient(
                        Gson(),
                        OkHttpClient(),
                        "http://localhost:${mockWebServer.port}"),
                StoredVersionParser(Fixtures.yamlParser()),
                Fixtures.yamlParser()
        )

        val result = subject.fetchTravisYaml()

        Assertions.assertThat(result.getOrThrow()).isEqualTo(Fixtures.travisYml)
    }

}
