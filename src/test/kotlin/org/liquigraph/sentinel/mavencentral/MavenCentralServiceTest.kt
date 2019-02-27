package org.liquigraph.sentinel.mavencentral

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.SemanticVersion
import org.liquigraph.sentinel.WatchedCoordinates
import org.liquigraph.sentinel.toVersion

class MavenCentralServiceTest {
    private val mavenCentralClient = mock<MavenCentralClient>()
    private val subject = MavenCentralService(mavenCentralClient)
    private val watchedArtifact = mavenDefinition("org.neo4j", "neo4j", "jar", ".jar")

    @Test
    fun `returns version fetched by client`() {
        whenever(mavenCentralClient.fetchMavenCentralResults()).thenReturn(Success(
                listOf(
                        MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                        MavenArtifact("org.neo4j", "neo4j", "2.3.4".toVersion(), "jar", listOf(".jar"))
                )
        ))

        val neo4jVersions = subject.getArtifacts(watchedArtifact).getOrThrow()

        assertThat(neo4jVersions)
                .extracting<SemanticVersion?> { it.version }
                .containsExactly("1.2.3".toVersion(), "2.3.4".toVersion())
    }

    @Test
    fun `returns version fetched by client when they match the correct artifact attributes`() {
        whenever(mavenCentralClient.fetchMavenCentralResults()).thenReturn(Success(
                listOf(
                        MavenArtifact("org.neo4j2", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                        MavenArtifact("org.neo4j", "neo4s", "2.3.5".toVersion(), "jar", listOf(".jar")),
                        MavenArtifact("org.neo4j", "neo4j", "2.3.4".toVersion(), "jar", listOf(".jar")),
                        MavenArtifact("org.neo4j", "neo4j", "2.3.6".toVersion(), "jar", listOf("-tests.jar")),
                        MavenArtifact("org.neo4j", "neo4j", "2.3.4".toVersion(), "pom", listOf(".pom"))
                )
        ))

        val neo4jVersions = subject.getArtifacts(watchedArtifact).getOrThrow()

        assertThat(neo4jVersions)
                .extracting<SemanticVersion?> { it.version }
                .containsExactly("2.3.4".toVersion())
    }

    @Test
    fun `propagates the client error`() {
        whenever(mavenCentralClient.fetchMavenCentralResults()).thenReturn(Failure(404, "Not Found"))

        val result = subject.getArtifacts(watchedArtifact)

        assertThat(result).isEqualTo(Failure<List<String>>(404, "Not Found"))
    }

    private fun mavenDefinition(groupId: String, artifactId: String, packaging: String, classifier: String): WatchedCoordinates.MavenCoordinates {
        val result = WatchedCoordinates.MavenCoordinates()
        result.groupId = groupId
        result.artifactId = artifactId
        result.packaging = packaging
        result.classifier = classifier
        return result
    }

}
