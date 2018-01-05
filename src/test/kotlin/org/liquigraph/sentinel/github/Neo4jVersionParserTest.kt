package org.liquigraph.sentinel.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class Neo4jVersionParserTest {

    @Test
    fun parses() {
        val parser = Neo4jVersionParser(Yaml())

        val versions: List<Neo4jVersion> = parser.parse("""
            |env:
            |  matrix:
            |    - NEO_VERSION=1.2.3
            |      WITH_DOCKER=true
            |    - NEO_VERSION=3.2.1
            |    - NEO_VERSION=2.2.2
            |      WITH_DOCKER=false
        """.trimMargin())

        assertThat(versions)
                .containsOnlyOnce(
                        Neo4jVersion("1.2.3", inDockerStore = true),
                        Neo4jVersion("2.2.2", inDockerStore = false),
                        Neo4jVersion("3.2.1", inDockerStore = false))

    }

}