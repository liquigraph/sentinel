package org.liquigraph.sentinel.github

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.getContentOrThrow
import org.liquigraph.sentinel.toVersion
import org.mockito.Matchers.anyString
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

class RawTravisYamlServiceTest {

    private val travisYamlClient = mock<TravisYamlClient>()
    private val neo4jVersionParser = mock<TravisNeo4jVersionParser>()
    private lateinit var liquigraphService : TravisYamlService
    private lateinit var yamlParser : Yaml

    @Before
    fun `set up`() {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        yamlParser = Yaml(options)
        liquigraphService = TravisYamlService(travisYamlClient, neo4jVersionParser, yamlParser)


        whenever(neo4jVersionParser.parse(anyString()) ).thenReturn(Success(listOf(TravisNeo4jVersion("3.0.11".toVersion(), true), //
                                                                                  TravisNeo4jVersion("3.1.7".toVersion())))
        )

        whenever(travisYamlClient.fetchTravisYaml()).thenReturn(Success(Fixtures.travisYml))
    }

    @Test
    fun `propagates the error`() {
        val expectedError = Failure<String>(666, "Nope")
        whenever(travisYamlClient.fetchTravisYaml()).thenReturn(expectedError)

        val result = liquigraphService.getNeo4jVersions()

        assertThat(result).isEqualTo(expectedError)
    }

    @Test
    fun `retrieves the neo4j versions`() {
        val neo4jVersions = listOf(TravisNeo4jVersion("1.2.3", true))
        whenever(neo4jVersionParser.parse(Fixtures.travisYml)).thenReturn(Success(neo4jVersions))

        val result = liquigraphService.getNeo4jVersions()

        assertThat(result).isEqualTo(Success(neo4jVersions))
    }

    @Test
    fun `update yaml with new version addition` () {
        val yaml = Fixtures.travisYml
        val content = yamlParser.load<MutableMap<String, Any>>(yaml)

        val result = liquigraphService.update(listOf(Addition("4.0.0".toVersion(), true),
                Addition("5.0.0".toVersion(), false)))

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getContentOrThrow())

        val updatedEnv = updatedContent.remove("env") as Map<String, List<String>>
        val env = content.remove("env") as Map<String, List<String>>

        assertThat(updatedContent).isEqualTo(content)
        assertThat(updatedEnv["matrix"])
                .containsAll(env["matrix"])
                .contains("NEO_VERSION=4.0.0 WITH_DOCKER=true", "NEO_VERSION=5.0.0 WITH_DOCKER=false")
    }

    @Test
    fun `updated version should preserved order` () {
        val result = liquigraphService.update(listOf(Addition("3.0.12".toVersion(), false)))

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getContentOrThrow())

        val updatedEnv = updatedContent.remove("env") as Map<String, List<String>>

        assertThat(updatedEnv["matrix"])
                .containsExactly(
                        "NEO_VERSION=3.0.11 WITH_DOCKER=true",
                        "NEO_VERSION=3.0.12 WITH_DOCKER=false",
                        "NEO_VERSION=3.1.7 WITH_DOCKER=false")
    }
}
