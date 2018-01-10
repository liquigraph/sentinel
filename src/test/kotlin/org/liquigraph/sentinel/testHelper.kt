package org.liquigraph.sentinel

import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.github.SemanticVersion

fun <T> Result<T>.getContentOrThrow() =
        (this as? Success)?.content ?: throw RuntimeException("Failure: " + this)

fun String.toVersion() = SemanticVersion.parse(this)!!