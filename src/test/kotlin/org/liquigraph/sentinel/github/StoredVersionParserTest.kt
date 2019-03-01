package org.liquigraph.sentinel.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class StoredVersionParserTest {

    private val parser = StoredVersionParser(Yaml())

    @Test
    fun `parses versions`() {
        val versions = parser.parse("""
            |env:
            |  matrix:
            |    - NEO_VERSION=1.2.3
            |      WITH_DOCKER=true
            |    - NEO_VERSION=2.2.2
            |      WITH_DOCKER=false
        """.trimMargin())

        assertThat(versions.getOrThrow())
                .containsOnlyOnce(
                        StoredVersion("1.2.3", inDockerStore = true),
                        StoredVersion("2.2.2", inDockerStore = false))
    }

    @Test
    fun `parses versions without explicit Docker setting`() {
        val versions = parser.parse("""
            |env:
            |  matrix:
            |    - NEO_VERSION=1.2.3
        """.trimMargin())

        assertThat(versions.getOrThrow()).containsOnlyOnce(StoredVersion("1.2.3", inDockerStore = false))
    }

    @Test
    fun `returns an error when there is no NEO_VERSION`() {
        val error = parser.parse("""
            |env:
            |  matrix:
            |    - WITH_DOCKER=true
        """.trimMargin())

        assertThat(error.exceptionOrNull()!!.message).isEqualTo("Missing 'NEO_VERSION' field at index 0")
    }

    @Test
    fun `returns an error when several rows have no NEO_VERSION`() {
        val error = parser.parse("""
            |env:
            |  matrix:
            |    - WITH_DOCKER=true
            |    - WITH_DOCKER=false
            |    - WITH_DOCKER=true
        """.trimMargin())

        assertThat(error.exceptionOrNull()!!.message).isEqualTo("""Missing 'NEO_VERSION' field at index 0
            |Missing 'NEO_VERSION' field at index 1
            |Missing 'NEO_VERSION' field at index 2
        """.trimMargin())
    }
}