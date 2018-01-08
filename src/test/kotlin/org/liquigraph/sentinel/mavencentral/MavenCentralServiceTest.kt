package org.liquigraph.sentinel.mavencentral

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.getContentOrThrow
import org.liquigraph.sentinel.model.Failure
import org.liquigraph.sentinel.model.MavenCentralArtifact
import org.liquigraph.sentinel.model.Success

class MavenCentralServiceTest {
    val mavenCentralClient = mock<MavenCentralClient>()
    val subject = MavenCentralService(mavenCentralClient)

    @Test
    fun `returns version fetched by client`() {
        whenever(mavenCentralClient.fetchMavenCentralResults()).thenReturn(Success(
                listOf(
                        MavenCentralArtifact("org.neo4j", "neo4j", "1.2.3", "jar", listOf(".jar")),
                        MavenCentralArtifact("org.neo4j", "neo4j", "2.3.4", "jar", listOf(".jar"))
                )
        ))

        val neo4jVersions = subject.getNeo4jArtifacts().getContentOrThrow()

        assertThat(neo4jVersions).extracting { it.version }.containsExactly("1.2.3", "2.3.4")
    }

    @Test
    fun `returns version fetched by client when they match the correct artifact attributes`() {
        whenever(mavenCentralClient.fetchMavenCentralResults()).thenReturn(Success(
                listOf(
                        MavenCentralArtifact("org.neo4j2", "neo4j", "1.2.3", "jar", listOf(".jar")),
                        MavenCentralArtifact("org.neo4j", "neo4s", "2.3.5", "jar", listOf(".jar")),
                        MavenCentralArtifact("org.neo4j", "neo4j", "2.3.4", "jar", listOf(".jar")),
                        MavenCentralArtifact("org.neo4j", "neo4j", "2.3.6", "jar", listOf("-tests.jar")),
                        MavenCentralArtifact("org.neo4j", "neo4j", "2.3.4", "pom", listOf(".pom"))
                )
        ))

        val neo4jVersions = subject.getNeo4jArtifacts().getContentOrThrow()

        assertThat(neo4jVersions).extracting { it.version }.containsExactly("2.3.4")
    }

    @Test
    fun `propagates the client error`() {
        whenever(mavenCentralClient.fetchMavenCentralResults()).thenReturn(Failure(404, "Not Found"))

        val result = subject.getNeo4jArtifacts()

        assertThat(result).isEqualTo(Failure<List<String>>(404, "Not Found"))
    }
}