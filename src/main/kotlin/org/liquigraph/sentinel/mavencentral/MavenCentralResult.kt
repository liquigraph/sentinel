package org.liquigraph.sentinel.mavencentral

import com.google.gson.annotations.SerializedName
import org.liquigraph.sentinel.github.SemanticVersion

data class MavenCentralResult(var response: MavenCentralResponse?)
data class MavenCentralResponse(var docs: List<MavenArtifact>?)
data class MavenArtifact(
        @SerializedName("g")
        var groupId: String,
        @SerializedName("a")
        var artifactId: String,
        @SerializedName("v")
        var version: SemanticVersion?,
        @SerializedName("p")
        var packaging: String,
        @SerializedName("ec")
        var classifiers: List<String>
) {
        override fun toString(): String {
                return "$groupId:$artifactId:$version:$packaging (${classifiers.joinToString()})"
        }
}