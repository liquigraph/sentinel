package org.liquigraph.sentinel.mavencentral

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.liquigraph.sentinel.effects.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MavenCentralClient(private val httpClient: OkHttpClient,
                         private val gson: Gson,
                         @Value("\${mavenSearch.baseUri}") private val baseUri: String) {

    fun fetchMavenCentralResults(): Result<List<MavenArtifact>> {
        val responseBody = responseBody()

        return when (responseBody) {
            is Failure -> Failure(responseBody.code, responseBody.message)
            is Success -> extractDocs(responseBody)
        }
    }

    private fun extractDocs(responseBody: Success<String>): Result<List<MavenArtifact>> {
        return try {
            val result = gson.fromJson(responseBody.content, MavenCentralResult::class.java)
            val response = result.response

            if (response == null) {
                Failure(2002, "Could not find 'response' field")
            } else {
                val docs = response.docs
                if (docs == null) {
                    Failure(2002, "Could not find 'docs' field")
                } else {
                    Success(docs)
                }
            }
        } catch (e: JsonSyntaxException) {
            Failure(2001, e.message!!)
        }
    }

    private fun responseBody(): Result<String> {
        val response = httpClient.newCall(Request.Builder()
                .url("$baseUri/solrsearch/select?q=g%3A\"org.neo4j\"%20AND%20a%3A\"neo4j\"&core=gav&wt=json&rows=400")
                .build())
                .execute()

        return if (response.isSuccessful) {
            Success(response.body()!!.string())
        }
        else when (response.code()) {
            in 400..499 -> Failure(response.code(), "4xx error")
            in 500..599 -> Failure(response.code(), "Unreachable $baseUri")
            else -> Failure(response.code(), "Unexpected error")
        }
    }

}