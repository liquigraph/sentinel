package org.liquigraph.sentinel.github

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Computation
import org.liquigraph.sentinel.effects.Success
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class StoredBuildClient(val gson: Gson,
                        val httpClient: OkHttpClient,
                        @Value("\${githubApi.baseUri}") val baseUri: String) {

    fun fetchBuildDefinition(): Computation<String> {
        val response = fetchFile("${baseUri}/repos/liquigraph/liquigraph/contents/.travis.yml")
        return try {
            decodeContent(response)
        }
        catch (e: JsonSyntaxException) {
            Failure(1001, e.message!!)
        }
    }

    private fun fetchFile(uri: String): Response {
        return httpClient.newCall(Request.Builder()
                .url(uri)
                .build())
                .execute()
    }

    private fun decodeContent(response: Response): Computation<String> {
        return if (!response.isSuccessful) {
            handleGithubErrors(response)
        } else {
            val body = bodyAsMap(response.body()!!)
            Success(decode(sanitize(body["content"]!!)))
        }
    }

    private fun handleGithubErrors(response: Response): Failure<String> {
        return when (response.code()) {
            in 400..499 -> Failure(response.code(), bodyAsMap(response.body()!!)["message"]!!)
            in 500..599 -> Failure(response.code(), "Unreachable $baseUri")
            else -> {
                Failure(response.code(), "Unexpected error")
            }
        }
    }

    private fun bodyAsMap(responseBody: ResponseBody): Map<String, String> {
        return gson.fromJson<Map<String, String>>(
                responseBody.string(),
                Map::class.java
        )
    }

    private fun sanitize(base64WithWeirdNewlines: String): String {
        return base64WithWeirdNewlines.replace("\n", "")
    }

    private fun decode(base64Content: String): String {
        return String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8)
    }
}