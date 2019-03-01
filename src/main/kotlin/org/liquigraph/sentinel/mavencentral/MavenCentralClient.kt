package org.liquigraph.sentinel.mavencentral

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.liquigraph.sentinel.effects.toResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

@Component
class MavenCentralClient(private val httpClient: OkHttpClient,
                         private val gson: Gson,
                         @Value("\${mavenSearch.baseUri}") final val baseUri: String) {

    private val uri = "$baseUri/solrsearch/select?q=g%3A\"org.neo4j\"%20AND%20a%3A\"neo4j\"&core=gav&wt=json&rows=400"

    fun fetchMavenCentralResults(): Result<List<MavenArtifact>> {
        return responseBody(uri).fold({ extractDocs(it) }, { Result.failure(it) })
    }

    private fun extractDocs(responseBody: String): Result<List<MavenArtifact>> {
        return try {
            val result = gson.fromJson(responseBody, MavenCentralResult::class.java)
            val response = result.response

            if (response == null) {
                Result.failure(IllegalStateException("Could not find 'response' field"))
            } else {
                val docs = response.docs
                if (docs == null) {
                    Result.failure(IllegalStateException("Could not find 'docs' field"))
                } else {
                    Result.success(docs)
                }
            }
        } catch (e: JsonSyntaxException) {
            Result.failure(e)
        }
    }

    private fun responseBody(uri: String): Result<String> {
        return httpClient.newCall(Request.Builder()
                .url(uri)
                .build())
                .execute()
                .toResult()
    }

}