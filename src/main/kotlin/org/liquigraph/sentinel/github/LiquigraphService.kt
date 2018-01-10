package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.springframework.stereotype.Service

sealed class VersionChange
data class Update(val old: SemanticVersion, val new: SemanticVersion) : VersionChange()
data class Addition(val new: SemanticVersion) : VersionChange()

@Service
class LiquigraphService {
    fun retainNewVersions(travisYmlVersions: List<TravisNeo4jVersion>, mavenArtifacts: List<MavenArtifact>): List<VersionChange> {
        val travisVersions = travisYmlVersions.map { it.version }
        val travisMajorVersions = travisVersions.map { it.major }
        return mavenArtifacts.map { it.version }
                .filter { travisMajorVersions.contains(it.major) }
                .filter { it.isStable() }
                .groupBy { Pair(it.major, it.minor) }
                .mapNotNull { (majorMinor, mavenVersions) ->
                    val mavenVersion = mavenVersions.max()!!
                    val travisVersion = getLatestUpdateableVersion(travisVersions, majorMinor.first, majorMinor.second)
                    when {
                        travisVersion == null -> Addition(mavenVersion)
                        travisVersion.compareTo(mavenVersion) != 0 -> Update(travisVersion, mavenVersion)
                        else -> null
                    }
                }
    }

    private fun getLatestUpdateableVersion(travisVersions: List<SemanticVersion>, major: Int, minor: Int) =
            travisVersions
                    .sortedDescending()
                    .dropLast(1) // we retain the lowest version to be confident we are compatible with, e.g., 3.0.0 -> 3.4.2
                    .find { it.major == major && it.minor == minor }

}