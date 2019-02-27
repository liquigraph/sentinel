package org.liquigraph.sentinel.effects

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

class ComputationTest {

    @Test
    fun `maps successes`() {
        val success = Success("foobar")

        val result = success.map { it.length }

        assertThat(result.content).isEqualTo("foobar".length)
    }

    @Test
    fun `maps failures`() {
        val failure = Failure<String>(503, "Oopsie")

        val result = failure.map { it.length }

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `flatmaps successes`() {
        val success = Success("foobar")

        val result = success.flatMap { Success(it.length) } as Success

        assertThat(result.content).isEqualTo("foobar".length)
    }

    @Test
    fun `flatmaps failures`() {
        val failure = Failure<String>(404, "Where art thou?")

        val result = failure.flatMap { Success(it.length) }

        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun `iterates on successes`() {
        val list = mutableListOf<String>()
        val success = Success("string")

        success.forEach { list.add(it) }

        assertThat(list).containsExactly("string")
    }

    @Test
    fun `throws when iterating on failures`() {
        val success = Failure<String>(2002, "nope nope")

        assertThatThrownBy {  success.forEach {  /* noop*/ } }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Computation failed with message nope nope and code 2002")
    }
}