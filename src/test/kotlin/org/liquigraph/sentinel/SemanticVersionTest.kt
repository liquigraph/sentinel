package org.liquigraph.sentinel

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.liquigraph.sentinel.SemanticVersion

class SemanticVersionTest {

    @Test
    fun `parses semantic stable versions`() {
        val version = SemanticVersion.parseEntire("1.2.3")!!

        assertThat(version.isStable()).isTrue()
        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(2)
        assertThat(version.patch).isEqualTo(3)
    }

    @Test
    fun `parses semantic pre-release versions`() {
        val version = SemanticVersion.parseEntire("1.2.3-alpha.4")!!

        assertThat(version.isStable()).isFalse()
        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(2)
        assertThat(version.patch).isEqualTo(3)
        assertThat(version.preRelease).isEqualTo(listOf("alpha", "4"))
    }

    @Test
    fun `does not parse non-semantic versions`() {
        assertThat(SemanticVersion.parseEntire("00.1.2")).isNull()
        assertThat(SemanticVersion.parseEntire("0.1")).isNull()
        assertThat(SemanticVersion.parseEntire("0.1.2.salut")).isNull()
    }

    @Test
    fun `compares versions`() {
        // see https://semver.org/spec/v2.0.0.html
        assertThat(SemanticVersion.parseEntire("1.2.3")!!).isLessThan(SemanticVersion.parseEntire("2.2.3")!!)
        assertThat(SemanticVersion.parseEntire("1.2.3")!!).isLessThan(SemanticVersion.parseEntire("1.3.3")!!)
        assertThat(SemanticVersion.parseEntire("1.2.3")!!).isLessThan(SemanticVersion.parseEntire("1.2.4")!!)
        assertThat(SemanticVersion.parseEntire("1.2.3-alpha")!!).isLessThan(SemanticVersion.parseEntire("1.2.3")!!)
        assertThat(SemanticVersion.parseEntire("1.2.3-alpha")!!).isLessThan(SemanticVersion.parseEntire("1.2.3-alpha.1")!!)
    }

    @Test
    fun `extract versions`() {
        val versions = SemanticVersion.extractAll("test 3.2.1, test 3.4.5")
        assertThat(versions).containsExactly(SemanticVersion(3, 2, 1, listOf()), SemanticVersion(3, 4, 5, listOf()))
    }
}