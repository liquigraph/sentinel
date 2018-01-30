package org.liquigraph.sentinel.github

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


        fun parseEntire(version: String): SemanticVersion? {
            val result = semanticVersionPattern.matchEntire(version)
            return when (result) {
                null -> null
                else -> semanticVersion(result.groups)
            }
        }

        fun extractAll(text: String, filter: (String) -> Boolean = ::alwaysTrue): List<SemanticVersion> {
            return semanticVersionPattern.findAll(text)
                    .filter { filter(it.value) }
                    .map { result -> semanticVersion(result.groups) }.toList()


        }

        private fun semanticVersion(capturedGroups: MatchGroupCollection): SemanticVersion {
            return SemanticVersion(
                    capturedGroups[1]!!.value.toInt(),
                    capturedGroups[2]!!.value.toInt(),
                    capturedGroups[3]!!.value.toInt(),
                    capturedGroups[4]?.value?.split('.') ?: emptyList()
            )
        }


        private fun <T> alwaysTrue(ignored: T) = true

    }
}