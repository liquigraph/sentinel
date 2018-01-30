package org.liquigraph.sentinel.github

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.Fixtures
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success

class RawTravisYamlServiceTest {

    private val travisYamlClient = mock<TravisYamlClient>()
    private val neo4jVersionParser = mock<TravisNeo4jVersionParser>()
    private val liquigraphService = TravisYamlService(travisYamlClient, neo4jVersionParser)

    @Test
    fun `propagates the error`() {
        val expectedError = Failure<String>(666, "Nope")
        whenever(travisYamlClient.fetchTravisYaml()).thenReturn(expectedError)

        val result = liquigraphService.getNeo4jVersions()

        assertThat(result).isEqualTo(expectedError)
    }

    @Test
    fun `retrieves the neo4j versions`() {
        whenever(travisYamlClient.fetchTravisYaml()).thenReturn(Success(Fixtures.travisYml))
        val neo4jVersions = listOf(TravisNeo4jVersion("1.2.3", true))
        whenever(neo4jVersionParser.parse(Fixtures.travisYml)).thenReturn(Success(neo4jVersions))

        val result = liquigraphService.getNeo4jVersions()

        assertThat(result).isEqualTo(Success(neo4jVersions))
    }
}