package org.liquigraph.sentinel.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.getContentOrThrow
import org.yaml.snakeyaml.Yaml
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success

class TravisNeo4jVersionParserTest {
    val parser = TravisNeo4jVersionParser(Yaml())

    @Test
    fun `parses versions`() {
        val versions = parser.parse("""
            |env:
            |  matrix:
            |    - NEO_VERSION=1.2.3
            |      WITH_DOCKER=true
            |    - NEO_VERSION=2.2.2
            |      WITH_DOCKER=false
        """.trimMargin()) as Success<List<TravisNeo4jVersion>>

        assertThat(versions.getContentOrThrow())
                .containsOnlyOnce(
                        TravisNeo4jVersion("1.2.3", inDockerStore = true),
                        TravisNeo4jVersion("2.2.2", inDockerStore = false))

    }

    @Test
    fun `parses versions without explicit Docker setting`() {
        val versions = parser.parse("""
            |env:
            |  matrix:
            |    - NEO_VERSION=1.2.3
        """.trimMargin()) as Success<List<TravisNeo4jVersion>>

        assertThat(versions.getContentOrThrow()).containsOnlyOnce(TravisNeo4jVersion("1.2.3", inDockerStore = false))

    }

    @Test
    fun `returns an error with invalid yaml`() {
        val error = parser.parse("!-- fkldsjfliewfoi") as Failure

        assertThat(error.code).isEqualTo(1000)
        assertThat(error.message).containsIgnoringCase("invalid")

    }

    @Test
    fun `returns an error without env field`() {
        val error = parser.parse("") as Failure

        assertThat(error.code).isEqualTo(1001)
        assertThat(error.message).isEqualTo("Could not find 'env' field")
    }

    @Test
    fun `returns an error without matrix field`() {
        val error = parser.parse("""
            |env:
            |  cloud-atlas:
            |    - NEO_VERSION=1.2.3
        """.trimMargin()) as Failure

        assertThat(error.code).isEqualTo(1001)
        assertThat(error.message).isEqualTo("Could not find 'matrix' field")
    }

    @Test
    fun `returns an error when there is no NEO_VERSION`() {
        val error = parser.parse("""
            |env:
            |  matrix:
            |    - WITH_DOCKER=true
        """.trimMargin()) as Failure

        assertThat(error.code).isEqualTo(1002)
        assertThat(error.message).isEqualTo("Missing 'NEO_VERSION' field at index 0")
    }

    @Test
    fun `returns an error when several rows have no NEO_VERSION`() {
        val error = parser.parse("""
            |env:
            |  matrix:
            |    - WITH_DOCKER=true
            |    - WITH_DOCKER=false
            |    - WITH_DOCKER=true
        """.trimMargin()) as Failure

        assertThat(error.code).isEqualTo(1002)
        assertThat(error.message).isEqualTo("""Missing 'NEO_VERSION' field at index 0
            |Missing 'NEO_VERSION' field at index 1
            |Missing 'NEO_VERSION' field at index 2
        """.trimMargin())
    }
}