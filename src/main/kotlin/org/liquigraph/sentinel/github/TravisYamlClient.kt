package org.liquigraph.sentinel.github

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.liquigraph.sentinel.model.Result
import org.liquigraph.sentinel.model.Success
import org.liquigraph.sentinel.model.Error
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class TravisYamlClient(val gson: Gson,
                       val httpClient: OkHttpClient,
                       @Value("\${githubApi.baseUri}") val baseUri: String) {

    fun fetchTravisYaml(): Result<String> {
        val response = httpClient.newCall(Request.Builder()
                .url("$baseUri/repos/liquigraph/liquigraph/contents/.travis.yml")
                .build())
                .execute()

        return if (!response.isSuccessful) {
            when (response.code()) {
                in 400..499 -> Error(response.code(), bodyAsMap(response.body()!!)["message"]!!)
                in 500..599 -> Error(response.code(), "Unreachable $baseUri")
                else -> {
                    Error(response.code(), "Unexpected error")
                }
            }
        }
        else {
            val body = bodyAsMap(response.body()!!)
            Success(decode(sanitize(body["content"]!!)))
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