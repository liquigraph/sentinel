package org.liquigraph.sentinel

import org.liquigraph.sentinel.model.Result
import org.liquigraph.sentinel.model.Success

fun <T> Result<T>.getContentOrThrow() =
        (this as? Success)?.content ?: throw RuntimeException("Failure: " + this)