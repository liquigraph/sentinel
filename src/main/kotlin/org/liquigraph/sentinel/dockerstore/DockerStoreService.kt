package org.liquigraph.sentinel.dockerstore

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Computation
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.SemanticVersion
import org.liquigraph.sentinel.configuration.WatchedCoordinates
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DockerStoreService(private val httpClient: OkHttpClient,
                         private val gson: Gson,
                         @Value("\${dockerStore.baseUri}") private val baseUri: String) {

    fun getVersions(dockerDefinition: WatchedCoordinates.DockerCoordinates): Computation<Set<SemanticVersion>> {
        val responseBody = responseBody("$baseUri/api/content/v1/products/images/${dockerDefinition.image}")

        return when (responseBody) {
            is Failure -> Failure(responseBody.code, responseBody.message)
            is Success -> extractImageVersions(responseBody)
        }
    }

    private fun extractImageVersions(responseBody: Success<String>): Computation<Set<SemanticVersion>> {
        return try {
            Success(extract(responseBody.content))
        } catch (e: JsonSyntaxException) {
            Failure(3001, e.message!!)
        }
    }

    private fun extract(responseBody: String): Set<SemanticVersion> {
        val result = gson.fromJson(responseBody, DockerVersion::class.java)
        return SemanticVersion.extractAll(result.fullDescription) { v -> !v.contains('-') }
                .distinct()
                .toSet()
    }

    private fun responseBody(uri: String): Computation<String> {
        val response = httpClient.newCall(Request.Builder()
                .url(uri)
                .build())
                .execute()

        return if (!response.isSuccessful) when (response.code()) {
            in 400..499 -> Failure(response.code(), "4xx error")
            in 500..599 -> Failure(response.code(), "Unreachable $baseUri")
            else -> Failure(response.code(), "Unexpected error")
        } else {
            Success(response.body()!!.string())
        }
    }

}