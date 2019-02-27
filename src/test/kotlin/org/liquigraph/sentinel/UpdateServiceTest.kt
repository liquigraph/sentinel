package org.liquigraph.sentinel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.liquigraph.sentinel.github.StoredVersion
import org.liquigraph.sentinel.mavencentral.MavenArtifact

class UpdateServiceTest {

    private val liquigraphService = UpdateService()

    @Test
    fun `matches Maven Central largest-in-branch versions only`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("1.0.0".toVersion(), false)
        )

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, setOf())

        assertThat(result).containsExactly(Addition("1.2.5".toVersion(), false))
    }

    @Test
    fun `matches Maven Central largest versions per minor version branch only`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.6".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("2.0.0".toVersion(), false)
        )

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, setOf())

        assertThat(result).containsExactly(Addition("2.1.6".toVersion(), false))
    }

    @Test
    fun `computes version changes within Travis version major range only`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.9".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.6".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.0.5".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("2.0.0".toVersion(), false),
                StoredVersion("2.0.4".toVersion(), false),
                StoredVersion("2.1.2".toVersion(), false)
        )

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, setOf())

        assertThat(result).containsExactly(
                Update("2.0.4".toVersion(), "2.0.9".toVersion(), false),
                Update("2.1.2".toVersion(), "2.1.6".toVersion(), false),
                Addition("2.2.5".toVersion(), false)
        )
    }

    @Test
    fun `adds the highest Maven version that is not stored`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.9".toVersion(), "jar", listOf(".jar"))
        )

        val storedVersions = listOf(
                StoredVersion("2.0.0".toVersion(), false)
        )

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, setOf())

        assertThat(result).containsExactly(
                Addition("2.0.9".toVersion(), false)
        )
    }

    @Test
    fun `updates versions where the latest is not in Travis`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.6".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("1.0.2".toVersion(), false),
                StoredVersion("1.2.4".toVersion(), false)
        )

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, setOf())

        assertThat(result).containsExactly(Update("1.2.4".toVersion(), "1.2.5".toVersion(), false))
    }

    @Test
    fun `excludes unstable versions`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.3-alpha05".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("1.0.0".toVersion(), false),
                StoredVersion("1.0.2".toVersion(), false)
        )

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, setOf())

        assertThat(result).isEmpty()
    }

    @Test
    fun `adds new Maven versions that are Dockerized`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(StoredVersion("1.0.0".toVersion(), false))
        val dockerizedVersions = setOf("1.2.3".toVersion())

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, dockerizedVersions)

        assertThat(result).containsExactly(Addition("1.2.3".toVersion(), dockerized = true))
    }

    @Test
    fun `updates versions that become dockerized`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("1.0.0".toVersion(), inDockerStore = false),
                StoredVersion("1.2.3".toVersion(), inDockerStore = false)
        )
        val dockerizedVersions = setOf("1.2.3".toVersion())

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, dockerizedVersions)

        assertThat(result).containsExactly(Update("1.2.3".toVersion(), "1.2.3".toVersion(), dockerized = true))
    }

    @Test
    fun `updates minimum stored version to itself if it becomes Dockerized`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("1.2.3".toVersion(), inDockerStore = false)
        )
        val dockerizedVersions = setOf("1.2.3".toVersion())

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, dockerizedVersions)

        assertThat(result).containsExactly(Update("1.2.3".toVersion(), "1.2.3".toVersion(), dockerized = true))
    }

    @Test
    fun `adds highest in-branch Maven version that is not Dockerized yet`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.5".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("1.0.0".toVersion(), inDockerStore = false),
                StoredVersion("1.0.4".toVersion(), inDockerStore = true)
        )
        val dockerizedVersions = setOf("1.0.4".toVersion())

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, dockerizedVersions)

        assertThat(result).containsExactly(Addition("1.0.5".toVersion(), dockerized = false))
    }

    @Test
    fun `updates the highest in-branch stored version to the highest in-branch Dockerized version`() {
        val mavenVersions = listOf(
                MavenArtifact("org.neo4j", "neo4j", "3.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.3.6".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.3.7".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.3.8".toVersion(), "jar", listOf(".jar"))
        )
        val storedVersions = listOf(
                StoredVersion("3.0.0".toVersion(), inDockerStore = true),
                StoredVersion("3.3.6".toVersion(), inDockerStore = true)
        )
        val dockerizedVersions = setOf("3.0.0".toVersion(), "3.3.6".toVersion(), "3.3.7".toVersion())

        val result = liquigraphService.computeVersionChanges(storedVersions, mavenVersions, dockerizedVersions)

        assertThat(result).containsExactly(
                Update("3.3.6".toVersion(), "3.3.7".toVersion(), true),
                Addition("3.3.8".toVersion(), dockerized = false))
    }


}