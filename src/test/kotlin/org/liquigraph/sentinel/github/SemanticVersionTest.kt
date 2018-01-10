package org.liquigraph.sentinel.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun `parses semantic stable versions`() {
        val version = SemanticVersion.parse("1.2.3")!!

        assertThat(version.isStable()).isTrue()
        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(2)
        assertThat(version.patch).isEqualTo(3)
    }

    @Test
    fun `parses semantic pre-release versions`() {
        val version = SemanticVersion.parse("1.2.3-alpha.4")!!

        assertThat(version.isStable()).isFalse()
        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(2)
        assertThat(version.patch).isEqualTo(3)
        assertThat(version.preRelease).isEqualTo(listOf("alpha", "4"))
    }

    @Test
    fun `does not parse non-semantic versions`() {
        assertThat(SemanticVersion.parse("00.1.2")).isNull()
        assertThat(SemanticVersion.parse("0.1")).isNull()
        assertThat(SemanticVersion.parse("0.1.2.salut")).isNull()
    }

    @Test
    fun `compares versions`() {
        // see https://semver.org/spec/v2.0.0.html
        assertThat(SemanticVersion.parse("1.2.3")!!).isLessThan(SemanticVersion.parse("2.2.3")!!)
        assertThat(SemanticVersion.parse("1.2.3")!!).isLessThan(SemanticVersion.parse("1.3.3")!!)
        assertThat(SemanticVersion.parse("1.2.3")!!).isLessThan(SemanticVersion.parse("1.2.4")!!)
        assertThat(SemanticVersion.parse("1.2.3-alpha")!!).isLessThan(SemanticVersion.parse("1.2.3")!!)
        assertThat(SemanticVersion.parse("1.2.3-alpha")!!).isLessThan(SemanticVersion.parse("1.2.3-alpha.1")!!)
    }
}