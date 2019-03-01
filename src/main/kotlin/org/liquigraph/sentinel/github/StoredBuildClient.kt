package org.liquigraph.sentinel.github

import com.google.gson.Gson
import com.google.gson.JsonParseException
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.liquigraph.sentinel.configuration.BotPullRequestSettings
import org.liquigraph.sentinel.configuration.WatchedGithubRepository
import org.liquigraph.sentinel.effects.flatMap
import org.liquigraph.sentinel.effects.toResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class StoredBuildClient(val gson: Gson,
                        val httpClient: OkHttpClient,
                        final val repository: WatchedGithubRepository,
                        @Value("\${githubApi.baseUri}") final val baseUri: String) {

    private val mediaType: MediaType = MediaType.parse("application/json;charset=utf-8")!!
    private val baseRepositoryUrl = "$baseUri/repos/${repository.organization}/${repository.repository}"

    fun fetchBuildDefinition(): Result<String> {
        return get("$baseRepositoryUrl/contents/.travis.yml").flatMap(jsonSafe(this::extractYaml))
    }


    fun postTravisYamlBlob(yaml: String): Result<Pair<String, String>> {
        val blob = BlobInput(Base64.getEncoder().encodeToString(yaml.toByteArray(StandardCharsets.UTF_8)), "base64")
        val response = post("$baseRepositoryUrl/git/blobs", mediaType, blob)
        return response.flatMap(jsonSafe(this::extractBlobResponse)).map {
            Pair(yaml, it)
        }
    }

    fun getMostRecentCommitHash(): Result<String> {
        return get("$baseRepositoryUrl/git/refs/heads/${repository.branch}").flatMap(jsonSafe(this::extractRefSha))
    }

    fun postNewTree(baseRef: String, contentSha: Pair<String, String>): Result<String> {
        val tree = Tree(baseRef, listOf(
                RefTreeObject(".travis.yml", TreeMode.FILE.mode, "blob", contentSha.second)
        ))
        return post("$baseRepositoryUrl/git/trees", mediaType, tree).flatMap(jsonSafe(this::extractTreeHash))
    }

    fun postNewRef(refName: String, treeSha1: String): Result<String> {
        val ref = RefCreationInput("refs/heads/$refName", treeSha1)
        return post("$baseRepositoryUrl/git/refs", mediaType, ref).flatMap(jsonSafe(this::extractRefName))

    }

    fun postNewPullRequest(ref: String, botPullRequestSettings: BotPullRequestSettings): Result<String> {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val pullRequest = PullRequestCreationInput(
                head = ref,
                title = botPullRequestSettings.title.replace("##date##", currentDateTime),
                body = botPullRequestSettings.message)
        val response = post("$baseRepositoryUrl/pulls", mediaType, pullRequest)
        return response.flatMap(jsonSafe(this::extractHtmlUrl))
    }

    fun postNewCommit(treeHash: String, baseHash: String, commitMessage: String): Result<String> {
        val jsonMedia = mediaType
        val commit = CommitCreationInput(commitMessage, treeHash, listOf(baseHash))
        return post("$baseRepositoryUrl/git/commits", jsonMedia, commit).flatMap(jsonSafe(this::extractCommitResponse))
    }

    private fun get(uri: String): Result<String> {
        return httpClient.newCall(builderAt(uri).build()).execute().toResult()
    }

    private fun <T> post(uri: String, mediaType: MediaType, content: T): Result<String> {
        val blob = gson.toJson(content)
        val rawAuthorization = "${repository.username}:${repository.authToken}"
        val requestBuilder = builderAt(uri).post(RequestBody.create(mediaType, blob))
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(rawAuthorization.toByteArray()))
                .build()
        return httpClient.newCall(requestBuilder).execute().toResult()
    }

    private fun builderAt(uri: String) = Request.Builder().url(uri)

    private fun <T> jsonSafe(fn: (String) -> Result<T>): (String) -> Result<T> {
        return {
            try {
                fn(it)
            } catch (e: JsonParseException) {
                Result.failure(e)
            }
        }
    }

    private fun extractYaml(response: String) =
            Result.success(decodeBase64(removeNewlines(gson.fromJson<Map<String, String>>(response, Map::class.java)["content"]!!)))

    private fun extractBlobResponse(response: String) =
            Result.success(gson.fromJson(response, BlobResponse::class.java).sha)

    private fun extractCommitResponse(response: String) =
            Result.success(gson.fromJson(response, CommitResponse::class.java).sha)

    private fun extractRefSha(response: String) =
            Result.success(gson.fromJson(response, RefResponse::class.java).`object`.sha)

    private fun extractTreeHash(response: String) =
    // TODO: assert that only .travis.yml is listed as file and its SHA-1 matches
            Result.success(gson.fromJson(response, TreeResponse::class.java).sha)

    private fun extractRefName(response: String) =
            Result.success(gson.fromJson(response, RefResponse::class.java).ref)

    private fun extractHtmlUrl(response: String) =
            Result.success(gson.fromJson(response, PullRequestResponse::class.java).html_url)

    private fun removeNewlines(base64WithWeirdNewlines: String): String {
        return base64WithWeirdNewlines.replace("\n", "")
    }

    private fun decodeBase64(content: String): String {
        return String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8)
    }
}


typealias Sha1 = String

data class BlobInput(val content: String, val encoding: String)

data class BlobResponse(val sha: Sha1)
private data class RefCreationInput(val ref: String, val sha: Sha1)

private data class CommitCreationInput(val message: String, val tree: Sha1, val parents: List<String>)
private data class RefResponse(val ref: String, val `object`: RefResponseObject)
private data class RefResponseObject(val sha: Sha1)
private data class CommitResponse(val sha: Sha1)
private data class Tree(val base_tree: Sha1, val tree: List<RefTreeObject>)

private data class RefTreeObject(val path: String, val mode: String, val type: String, val sha: Sha1)
private enum class TreeMode(val mode: String) {
    FILE("100644"), EXECUTABLE("100644"), SUBDIRECTORY("100644"), SUBMODULE("100644"), SYMLINK("100644")
}

private data class TreeResponse(val sha: Sha1)
private data class PullRequestCreationInput(val title: String,
                                            val body: String,
                                            val head: String,
                                            val base: String = "master",
                                            val maintainer_can_modify: Boolean = true)

private data class PullRequestResponse(val html_url: String)