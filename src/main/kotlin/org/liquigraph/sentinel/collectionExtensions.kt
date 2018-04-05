package org.liquigraph.sentinel



fun <T> Collection<T>.joinLines(): String = joinLines { it.toString() }
fun <T> Collection<T>.joinLines(f: (T) -> String): String = joinToString("\n") { f(it) }
fun <T> List<T>.padAndZip(other: List<T>, zero: T): List<Pair<T, T>> {
    val paddingSize = kotlin.math.max(size, other.size)
    return pad(paddingSize, zero)
            .zip(other.pad(paddingSize, zero))
}

private fun <T> List<T>.pad(paddingSize: Int, zero: T): List<T> {
    return this + (1..paddingSize - size).map { _ -> zero }
}