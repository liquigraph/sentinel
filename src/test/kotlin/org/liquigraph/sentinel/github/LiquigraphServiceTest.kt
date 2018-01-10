package org.liquigraph.sentinel.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.toVersion

class LiquigraphServiceTest {
    val liquigraphService = LiquigraphService()

    @Test
    fun `matches Maven Central largest versions only`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar"))
        )


        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.0".toVersion(), false)
        )

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).containsExactly(Addition("1.2.5".toVersion()))
    }

    @Test
    fun `matches Maven Central largest versions per minor version branch only`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.2".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.1.6".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.0".toVersion(), false),
                TravisNeo4jVersion("2.0.0".toVersion(), false)
        )

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).containsExactly(Addition("1.2.5".toVersion()), Addition("2.1.6".toVersion()))
    }

    @Test
    fun  `excludes versions out of Travis version range`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.4".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "1.2.5".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
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

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).containsExactly(
                Update("2.0.4".toVersion(), "2.0.9".toVersion()),
                Update("2.1.2".toVersion(), "2.1.6".toVersion()),
                Addition("2.2.5".toVersion())
        )
    }

    @Test
    fun  `excludes Travis minimum version upgrade`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.9".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("2.0.0".toVersion(), false),
                TravisNeo4jVersion("2.0.4".toVersion(), false)
        )

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).containsExactly(
                Update("2.0.4".toVersion(), "2.0.9".toVersion())
        )
    }

    @Test
    fun  `creates an addition if only the Travis minimum version is present`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "2.0.0".toVersion(), "jar", listOf(".jar")),
                MavenArtifact("org.neo4j", "neo4j", "2.0.9".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("2.0.0".toVersion(), false)
        )

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).containsExactly(
                Addition("2.0.9".toVersion())
        )
    }

    @Test
    fun `updates versions where the latest is not in Travis`() {
        val mavenArtifacts = listOf(
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

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).containsExactly(Update("1.2.4".toVersion(), "1.2.5".toVersion()))
    }

    @Test
    fun `excludes unstable versions`() {
        val mavenArtifacts = listOf(
                MavenArtifact("org.neo4j", "neo4j", "1.2.3-alpha05".toVersion(), "jar", listOf(".jar"))
        )

        val travisYmlVersions = listOf(
                TravisNeo4jVersion("1.0.0".toVersion(), false)
        )

        val result = liquigraphService.retainNewVersions(travisYmlVersions, mavenArtifacts)

        assertThat(result).isEmpty()
    }
}