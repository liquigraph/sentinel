package org.liquigraph.sentinel.effects

interface Functor<out T> {
    fun <U> map(function: (T) -> U): Result<U>
    fun getOrThrow(): T
}

sealed class Result<out T>: Functor<T> {
    abstract fun <U> flatMap(function: (T) -> Result<U>): Result<U>
}

data class Failure<out T>(val code: Int, val message: String) : Result<T>() {
    override fun getOrThrow(): T {
        throw IllegalStateException("Computation failed with message $message and code $code")
    }

    override fun <U> flatMap(function: (T) -> Result<U>) = Failure<U>(code, message)
    override fun <U> map(function: (T) -> U) = Failure<U>(code, message)
}

data class Success<out T>(val content: T) : Result<T>() {
    override fun getOrThrow(): T = content
    override fun <U> flatMap(function: (T) -> Result<U>) = function(content)
    override fun <U> map(function: (T) -> U) = Success(function(content))
}
