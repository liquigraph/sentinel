package org.liquigraph.sentinel.effects

import okhttp3.Response
import org.springframework.util.ReflectionUtils

fun <T, U> Result<T>.flatMap(function: (T) -> Result<U>): Result<U> {
    return if (isSuccess) {
        function(getOrNull()!!)
    } else {
        Result.failure(this.exceptionOrNull()!!)
    }
}

fun <T> Result<T>.forEach(consumer: (T) -> Unit) {
    if (isSuccess) {
        consumer(getOrNull()!!)
    }
}

/**
 * Splits current lists into two buckets:
 *  - the left/first pair item contains all the failures
 *  - the right/second pair item contains all the successes (what is "right")
 */
fun <T> List<Result<T>>.partition(): Pair<List<Result<T>>, List<Result<T>>> {
    val initialValue = Pair<List<Result<T>>, List<Result<T>>>(emptyList(), emptyList())
    return this.fold(initialValue) { (failures, successes), currentResult ->
        if (currentResult.isFailure) {
            Pair(failures + currentResult, successes)
        } else {
            Pair(failures, successes + currentResult)
        }
    }
}

fun Response.toResult(): Result<String> {
    val uri = request().url().toString()
    return if (isSuccessful) {
        Result.success(body()!!.string())
    } else {
        Result.failure(IllegalStateException("Call on $uri resulted in response code ${code()}"))
    }
}