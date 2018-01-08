package org.liquigraph.sentinel.model

import com.google.gson.annotations.SerializedName

data class MavenCentralResult(var response: MavenCentralResponse?)
data class MavenCentralResponse(var docs: List<MavenCentralArtifact>?)
data class MavenCentralArtifact(
        @SerializedName("g")
        var groupId: String,
        @SerializedName("a")
        var artifactId: String,
        @SerializedName("v")
        var version: String,
        @SerializedName("p")
        var packaging: String,
        @SerializedName("ec")
        var classifiers: List<String>
)