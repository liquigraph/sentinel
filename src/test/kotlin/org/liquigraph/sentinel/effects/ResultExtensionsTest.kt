package org.liquigraph.sentinel.effects

import net.jqwik.api.*
import java.util.*
import kotlin.math.absoluteValue

class ResultExtensionsTest {

    @Property
    fun `flatmap check -- monad left identity`(@ForAll value: String): Boolean {
        val initial = point(value)
        return initial.flatMap(::f) == f(value)
    }

    @Property
    fun `flatmap check -- monad right identity`(@ForAll value: String): Boolean {
        val initial = f(value)
        return initial.flatMap { point(it) } == initial
    }

    @Property
    fun `flatmap check -- monad associativity`(@ForAll value: String): Boolean {
        val initial = point(value)
        return initial.flatMap(::f).flatMap(::g) == initial.flatMap { f(it).flatMap(::g) }
    }

    @Property
    fun `iterates over success`(@ForAll("success") success: Result<Any>): Boolean {
        var i = 0

        success.forEach { i++ }

        return i == 1
    }

    @Property
    fun `does not iterate over failures`(@ForAll("failure") failure: Result<Any>): Boolean {
        var i = 0

        failure.forEach { i++ }

        return i == 0
    }

    @Property
    fun `partitions list of results`(@ForAll("successes") successes: List<Result<Any>>,
                                     @ForAll("failures") failures: List<Result<Any>>): Boolean {

        val inputs = (failures.toSet() + successes.toSet()).toList()

        val result = inputs.partition()

        return result.first == failures && result.second == successes
    }

    @Provide
    fun successes(): Arbitrary<List<Result<Any>>> {
        return Arbitraries.randomValue { random -> generateSuccesses(random) }
    }

    @Provide
    fun failures(): Arbitrary<List<Result<Any>>> {

        return Arbitraries.randomValue { random -> generateFailures(random) }
    }

    @Provide
    fun success(): Arbitrary<Result<Any>> {
        return Arbitraries.randomValue { random -> generateSuccess(random) }
    }

    @Provide
    fun failure(): Arbitrary<Result<Any>> {
        return Arbitraries.randomValue { random -> generateFailure(random) }
    }


    companion object {
        fun f(s: String) = Result.success("$s something")
        fun g(s: String) = Result.success("$s nonething")

        fun <T> point(x: T): Result<T> = Result.success(x)

        fun generateSuccesses(random: Random): List<Result<Any>> {
            return generate(random, ::generateSuccess)
        }

        fun generateFailures(random: Random): List<Result<Any>> {
            return generate(random, ::generateFailure)
        }

        fun generateSuccess(random: Random): Result<Any> = Result.success(random.nextInt().toString(10))

        fun generateFailure(random: Random): Result<Any> = Result.failure(Throwable(random.nextInt().toString(10)))

        private fun generate(random: Random, fn: (Random) -> Result<Any>): List<Result<Any>> {
            return (0..random.nextInt(10).absoluteValue).map { fn(random) }
        }
    }

}