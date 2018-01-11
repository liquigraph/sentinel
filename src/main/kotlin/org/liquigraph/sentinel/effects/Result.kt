package org.liquigraph.sentinel.effects

interface Functor<out T> {
    fun <U> map(function: (T) -> U): Result<U>
}

sealed class Result<out T>: Functor<T> {
    abstract fun <U> flatMap(function: (T) -> Result<U>): Result<U>
}

data class Failure<out T>(val code: Int, val message: String) : Result<T>() {
    override fun <U> flatMap(function: (T) -> Result<U>) = Failure<U>(code, message)
    override fun <U> map(function: (T) -> U) = Failure<U>(code, message)
}

data class Success<out T>(val content: T) : Result<T>() {
    override fun <U> flatMap(function: (T) -> Result<U>) = function(content)
    override fun <U> map(function: (T) -> U) = Success(function(content))
}
