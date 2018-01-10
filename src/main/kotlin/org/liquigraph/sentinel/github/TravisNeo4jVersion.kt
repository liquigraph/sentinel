package org.liquigraph.sentinel.github

import kotlin.math.max

data class TravisNeo4jVersion(val version: SemanticVersion, val inDockerStore: Boolean) {
    constructor(version: String, inDockerStore: Boolean) : this(SemanticVersion.parse(version)!!, inDockerStore)
}

data class SemanticVersion(val major: Int,
                           val minor: Int,
                           val patch: Int,
                           val preRelease: List<String>) : Comparable<SemanticVersion> {

    fun isStable(): Boolean {
        return preRelease.isEmpty()
    }

    override fun compareTo(other: SemanticVersion): Int {
        val majorDelta = major - other.major
        return if (majorDelta != 0) majorDelta
        else {
            val minorDelta = minor - other.minor
            if (minorDelta != 0) minorDelta
            else {
                val patchDelta = patch - other.patch
                if (patchDelta != 0) patchDelta
                else comparePreReleases(other)
            }
        }
    }

    override fun toString(): String {
        return "$major.$minor.$patch${if (preRelease.isEmpty()) "" else "-" + preRelease.joinToString(".")}"
    }

    private fun comparePreReleases(other: SemanticVersion): Int {
        return when {
            preRelease.isEmpty() && other.preRelease.isNotEmpty() -> 1
            preRelease.isNotEmpty() && other.preRelease.isEmpty() -> -1
            else -> {
                val remainingIdentifiers = preRelease.padAndZip(other.preRelease, "")
                        .dropWhile { (x1, x2) -> x1.compareTo(x2) == 0 }
                when {
                    remainingIdentifiers.isEmpty() -> 0
                    else -> {
                        val pair = remainingIdentifiers.first()
                        pair.first.compareTo(pair.second)
                    }
                }
            }
        }
    }

    companion object {
        private val semanticVersionPattern =
                Regex("(0|(?:[1-9]\\d*))\\.(0|(?:[1-9]\\d*))\\.(0|(?:[1-9]\\d*))(?:-((?:[0-9A-Za-z-]+\\.?)*))?")

        fun parse(version: String): SemanticVersion? {
            val result = semanticVersionPattern.matchEntire(version)
            return when (result) {
                null -> null
                else -> {
                    val capturedGroups = result.groups
                    SemanticVersion(
                            capturedGroups[1]!!.value.toInt(),
                            capturedGroups[2]!!.value.toInt(),
                            capturedGroups[3]!!.value.toInt(),
                            capturedGroups[4]?.value?.split('.') ?: emptyList()
                    )
                }
            }
        }
    }
}

fun <T> List<T>.padAndZip(other: List<T>, zero: T): List<Pair<T, T>> {
    val paddingSize = max(size, other.size)
    return pad(paddingSize, zero)
            .zip(other.pad(paddingSize, zero))
}

private fun <T> List<T>.pad(paddingSize: Int, zero: T): List<T> {
    return this + (1..paddingSize - size).map { _ -> zero }
}