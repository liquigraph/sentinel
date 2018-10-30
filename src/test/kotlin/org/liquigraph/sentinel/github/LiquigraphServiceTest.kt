package org.liquigraph.sentinel.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.toVersion

class LiquigraphServiceTest {

    private val liquigraphService = LiquigraphService()

    @Test
    fun `matches Maven Central largest versions only`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar"))
        )


        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.0".toVersion(), false)
        )

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).containsExactly(Addition("1.2.5".toVersion(), false))
    }

    @Test
    fun `matches Maven Central largest versions per minor version branch only`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.6".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("2.0.0".toVersion(), false)
        )

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).containsExactly(Addition("2.1.6".toVersion(), false))
    }

    @Test
    fun `excludes versions out of Travis version range`() {
        val mavenArtifacts = listOf(
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

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("2.0.0".toVersion(), false),
                TravisNeo4jVersion("2.0.4".toVersion(), false),
                TravisNeo4jVersion("2.1.2".toVersion(), false)
        )

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).containsExactly(
                Update("2.0.4".toVersion(), "2.0.9".toVersion(), false),
                Update("2.1.2".toVersion(), "2.1.6".toVersion(), false),
                Addition("2.2.5".toVersion(), false)
        )
    }

    @Test
    fun `excludes Travis minimum version upgrade`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.9".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("2.0.0".toVersion(), false),
                TravisNeo4jVersion("2.0.4".toVersion(), false)
        )

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).containsExactly(
                Update("2.0.4".toVersion(), "2.0.9".toVersion(), false)
        )
    }

    @Test
    fun `creates an addition if only the Travis minimum version is present`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.9".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("2.0.0".toVersion(), false)
        )

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).containsExactly(
                Addition("2.0.9".toVersion(), false)
        )
    }

    @Test
    fun `updates versions where the latest is not in Travis`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.6".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.2".toVersion(), false),
                TravisNeo4jVersion("1.2.4".toVersion(), false),
                TravisNeo4jVersion("2.1.6".toVersion(), false)
        )

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).containsExactly(Update("1.2.4".toVersion(), "1.2.5".toVersion(), false))
    }

    @Test
    fun `excludes unstable versions`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.3-alpha05".toVersion(), "jar", listOf(".jar"))
        )
        val travisYmlVersions = listOf(TravisNeo4jVersion("1.0.0".toVersion(), false))

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, setOf())

        assertThat(result).isEmpty()
    }

    @Test
    fun `adds versions that become dockerized`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar"))
        )
        val travisYmlVersions = listOf(TravisNeo4jVersion("1.0.0".toVersion(), false))
        val dockerizedVersions = setOf("1.2.3".toVersion())

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, dockerizedVersions)

        assertThat(result).containsExactly(Addition("1.2.3".toVersion(), dockerized = true))
    }

    @Test
    fun `updates versions that become dockerized`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar"))
        )
        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.0".toVersion(), inDockerStore = false),
                TravisNeo4jVersion("1.2.3".toVersion(), inDockerStore = false)
        )
        val dockerizedVersions = setOf("1.2.3".toVersion())

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, dockerizedVersions)

        assertThat(result).containsExactly(Update("1.2.3".toVersion(), "1.2.3".toVersion(), dockerized = true))
    }

    @Test
    fun `Dockerizes minimum Travis version`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar"))
        )
        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.2.3".toVersion(), inDockerStore = false)
        )
        val dockerizedVersions = setOf("1.2.3".toVersion())

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, dockerizedVersions)

        assertThat(result).containsExactly(Update("1.2.3".toVersion(), "1.2.3".toVersion(), dockerized = true))
    }

    @Test
    fun `appends newest non-Dockerized version after latest Dockerized one`() {
        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.0".toVersion(), inDockerStore = false), // absolute minimum
                TravisNeo4jVersion("1.0.4".toVersion(), inDockerStore = true)
        )
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.0.5".toVersion(), "jar", listOf(".jar"))
        )
        val dockerizedVersions = setOf("1.0.4".toVersion())

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, dockerizedVersions)

        assertThat(result).containsExactly(Addition("1.0.5".toVersion(), dockerized = false))
    }

    @Test
    fun `should update to the latest dockerized for minor version`() {

        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "3.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.3.6".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.3.7".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "3.3.8".toVersion(), "jar", listOf(".jar"))
        )
        val travisYmlVersions = listOf(
                TravisNeo4jVersion("3.0.0".toVersion(), inDockerStore = true),
                TravisNeo4jVersion("3.3.6".toVersion(), inDockerStore = true)
        )

        val dockerizedVersions = setOf("3.0.0".toVersion(), "3.3.6".toVersion(), "3.3.7".toVersion())

        val result = liquigraphService.computeChanges(travisYmlVersions, mavenArtifacts, dockerizedVersions)

        assertThat(result).containsExactly(Update("3.3.6".toVersion(), "3.3.7".toVersion(), true),
                Addition("3.3.8".toVersion(), dockerized = false))
    }


}