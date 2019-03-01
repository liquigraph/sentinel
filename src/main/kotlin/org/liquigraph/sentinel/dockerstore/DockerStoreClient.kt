package org.liquigraph.sentinel.dockerstore

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.liquigraph.sentinel.SemanticVersion
import org.liquigraph.sentinel.configuration.WatchedArtifact
import org.liquigraph.sentinel.effects.toResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DockerStoreClient(private val httpClient: OkHttpClient,
                        private val gson: Gson,
                        @Value("\${dockerStore.baseUri}") private val baseUri: String) {

    fun getVersions(dockerDefinition: WatchedArtifact.DockerCoordinates): Result<Set<SemanticVersion>> {
        return responseBody("$baseUri/api/content/v1/products/images/${dockerDefinition.image}")
                .fold({ extractImageVersions(it) }, { Result.failure(it) })
    }

    private fun extractImageVersions(responseBody: String): Result<Set<SemanticVersion>> {
        return try {
            Result.success(extract(responseBody))
        } catch (e: JsonSyntaxException) {
            Result.failure(e)
        }
    }

    private fun extract(responseBody: String): Set<SemanticVersion> {
        val result = gson.fromJson(responseBody, DockerVersion::class.java)
        return SemanticVersion.extractAll(result.fullDescription) { v -> !v.contains('-') }
                .distinct()
                .toSet()
    }

    private fun responseBody(uri: String): Result<String> {
        return httpClient.newCall(Request.Builder()
                .url(uri)
                .build())
                .execute()
                .toResult()
    }

}