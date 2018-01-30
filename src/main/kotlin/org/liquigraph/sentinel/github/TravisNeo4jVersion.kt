package org.liquigraph.sentinel.github

import kotlin.math.max

data class TravisNeo4jVersion(val version: SemanticVersion, val inDockerStore: Boolean = false) {
    constructor(version: String, inDockerStore: Boolean) : this(SemanticVersion.parseEntire(version)!!, inDockerStore)
}

fun <T> List<T>.padAndZip(other: List<T>, zero: T): List<Pair<T, T>> {
    val paddingSize = max(size, other.size)
    return pad(paddingSize, zero)
            .zip(other.pad(paddingSize, zero))
}

private fun <T> List<T>.pad(paddingSize: Int, zero: T): List<T> {
    return this + (1..paddingSize - size).map { _ -> zero }
}