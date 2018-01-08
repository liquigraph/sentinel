package org.liquigraph.sentinel.model

sealed class Result<out T>
data class Failure<out T>(val code: Int, val message: String) : Result<T>()
data class Success<out T>(val content: T) : Result<T>()