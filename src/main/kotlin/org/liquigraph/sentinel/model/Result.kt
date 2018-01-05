package org.liquigraph.sentinel.model

sealed class Result<out T> {
    abstract fun isSuccessful(): Boolean
    abstract fun getContent(): T
}

data class Error<out T>(val code: Int, val message: String) : Result<T>() {
    override fun isSuccessful(): Boolean = false

    override fun getContent(): T {
        throw RuntimeException("nope")
    }
}

data class Success<out T>(private val content: T) : Result<T>() {
    override fun isSuccessful(): Boolean = true

    override fun getContent(): T {
        return content
    }
}