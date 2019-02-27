package org.liquigraph.sentinel.github

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.liquigraph.sentinel.Addition
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.Update
import org.liquigraph.sentinel.configuration.BotPullRequestSettings
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.toVersion
import org.mockito.ArgumentMatchers.anyString
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

class StoredVersionServiceTest {

    private val travisYamlClient = mock<StoredBuildClient>()
    private val neo4jVersionParser = mock<StoredVersionParser>()
    private lateinit var liquigraphService: StoredVersionService
    private lateinit var yamlParser: Yaml

    @BeforeEach
    fun `set up`() {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        yamlParser = Yaml(options)
        liquigraphService = StoredVersionService(travisYamlClient, neo4jVersionParser, BotPullRequestSettings(), yamlParser)

        whenever(neo4jVersionParser.parse(anyString()))
                .thenReturn(Success(listOf(StoredVersion("3.0.11".toVersion(), true), StoredVersion("3.1.7".toVersion()))))

        whenever(travisYamlClient.fetchBuildDefinition())
                .thenReturn(Success(Fixtures.travisYml))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `update yaml with new version addition`() {
        val yaml = Fixtures.travisYml
        val content = yamlParser.load<MutableMap<String, Any>>(yaml)

        val result = liquigraphService.applyChanges(
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
    @Suppress("UNCHECKED_CAST")
    fun `updated versions should preserve order of additions`() {
        val newNonDockerizedVersion = listOf(Addition("3.0.12".toVersion(), false))

        val result = liquigraphService.applyChanges(Fixtures.travisYml, newNonDockerizedVersion)

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getOrThrow())
        val matrixAfterUpdate = (updatedContent["env"] as Map<String, List<String>>)["matrix"]

        assertThat(matrixAfterUpdate).containsExactly(
                "NEO_VERSION=3.0.11 WITH_DOCKER=true",
                "NEO_VERSION=3.0.12 WITH_DOCKER=false",
                "NEO_VERSION=3.1.7 WITH_DOCKER=false")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should apply updates in order`() {
        val newVersions = listOf(
                Addition("3.2.8".toVersion(), false),
                Update("3.1.7".toVersion(), "3.1.9".toVersion(), true))

        val result = liquigraphService.applyChanges(Fixtures.travisYml, newVersions)

        val updatedContent = yamlParser.load<MutableMap<String, Any>>(result.getOrThrow())
        val matrixAfterUpdate = (updatedContent["env"] as Map<String, List<String>>)["matrix"]

        assertThat(matrixAfterUpdate).containsExactly(
                "NEO_VERSION=3.0.11 WITH_DOCKER=true",
                "NEO_VERSION=3.1.9 WITH_DOCKER=true",
                "NEO_VERSION=3.2.8 WITH_DOCKER=false")
    }
}
