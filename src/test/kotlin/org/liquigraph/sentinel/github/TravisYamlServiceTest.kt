package org.liquigraph.sentinel.github

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.toVersion
import org.mockito.Matchers.anyString
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

class TravisYamlServiceTest {

    private val travisYamlClient = mock<TravisYamlClient>()
    private val neo4jVersionParser = mock<TravisNeo4jVersionParser>()
    private lateinit var liquigraphService: TravisYamlService
    private lateinit var yamlParser: Yaml

    @Before
    fun `set up`() {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        yamlParser = Yaml(options)
        liquigraphService = TravisYamlService(travisYamlClient, neo4jVersionParser, yamlParser)

        whenever(neo4jVersionParser.parse(anyString()))
                .thenReturn(Success(listOf(TravisNeo4jVersion("3.0.11".toVersion(), true), TravisNeo4jVersion("3.1.7".toVersion()))))

        whenever(travisYamlClient.fetchTravisYaml())
                .thenReturn(Success(Fixtures.travisYml))
    }

    @Test
    fun `update yaml with new version addition`() {
        val yaml = Fixtures.travisYml
        val content = yamlParser.load<MutableMap<String, Any>>(yaml)

        val result = liquigraphService.update(
                Fixtures.travisYml,
                listOf(Addition("4.0.0".toVersion(), true),
                        Addition("5.0.0".toVersion(), false)))

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getOrThrow())

        val updatedEnv = updatedContent.remove("env") as Map<String, List<String>>
        val env = content.remove("env") as Map<String, List<String>>

        assertThat(updatedContent).isEqualTo(content)
        assertThat(updatedEnv["matrix"])
                .containsAll(env["matrix"])
                .contains("NEO_VERSION=4.0.0 WITH_DOCKER=true", "NEO_VERSION=5.0.0 WITH_DOCKER=false")
    }

    @Test
    fun `updated versions should preserve order of additions`() {
        val newNonDockerizedVersion = listOf(Addition("3.0.12".toVersion(), false))

        val result = liquigraphService.update(Fixtures.travisYml, newNonDockerizedVersion)

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getOrThrow())
        val matrixAfterUpdate = (updatedContent["env"] as Map<String, List<String>>)["matrix"]

        assertThat(matrixAfterUpdate).containsExactly(
                "NEO_VERSION=3.0.11 WITH_DOCKER=true",
                "NEO_VERSION=3.0.12 WITH_DOCKER=false",
                "NEO_VERSION=3.1.7 WITH_DOCKER=false")
    }

    @Test
    fun `should apply updates in order`() {
        val newVersions = listOf(
                Addition("3.2.8".toVersion(), false),
                Update("3.1.7".toVersion(), "3.1.9".toVersion(), true))

        val result = liquigraphService.update(Fixtures.travisYml, newVersions)

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getOrThrow())
        val matrixAfterUpdate = (updatedContent["env"] as Map<String, List<String>>)["matrix"]

        assertThat(matrixAfterUpdate).containsExactly(
                "NEO_VERSION=3.0.11 WITH_DOCKER=true",
                "NEO_VERSION=3.1.9 WITH_DOCKER=true",
                "NEO_VERSION=3.2.8 WITH_DOCKER=false")
    }
}