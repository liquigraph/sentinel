package org.liquigraph.sentinel.effects

interface Functor<out T> {
    fun <U> map(function: (T) -> U): Computation<U>
    fun getOrThrow(): T
}

sealed class Computation<out T> : Functor<T> {
    abstract fun <U> flatMap(function: (T) -> Computation<U>): Computation<U>
    abstract fun forEach(function: (T) -> Unit)
}

data class Failure<out T>(val code: Int, val message: String) : Computation<T>() {
    override fun getOrThrow(): T {
        throw IllegalStateException("Computation failed with message $message and code $code")
    }

    override fun <U> flatMap(function: (T) -> Computation<U>) = Failure<U>(code, message)
    override fun <U> map(function: (T) -> U) = Failure<U>(code, message)
    override fun forEach(function: (T) -> Unit) {
        throw IllegalStateException("Computation failed with message $message and code $code")
    }
}

data class Success<out T>(val content: T) : Computation<T>() {
    override fun getOrThrow(): T = content
    override fun <U> flatMap(function: (T) -> Computation<U>) = function(content)
    override fun <U> map(function: (T) -> U) = Success(function(content))
    override fun forEach(function: (T) -> Unit) {
        function(content)
    }
}


class CallError(code: Int, message: String) : Throwable("Call failed with message $message and code $code")