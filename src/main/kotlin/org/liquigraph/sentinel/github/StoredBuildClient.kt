package org.liquigraph.sentinel.github

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import org.liquigraph.sentinel.configuration.BotPullRequestSettings
import org.liquigraph.sentinel.configuration.WatchedGithubRepository
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Computation
import org.liquigraph.sentinel.effects.Success
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

    fun fetchBuildDefinition(): Computation<String> {
        val response = get("$baseRepositoryUrl/contents/.travis.yml")
        return mapResponse(response, this::extractYaml)
    }


    fun postTravisYamlBlob(yaml: String): Computation<Pair<String, String>> {
        val blob = BlobInput(Base64.getEncoder().encodeToString(yaml.toByteArray(StandardCharsets.UTF_8)), "base64")
        val response = post("$baseRepositoryUrl/git/blobs", mediaType, blob)
        return mapResponse(response, this::extractBlobResponse).map {
            Pair(yaml, it)
        }
    }

    fun getMostRecentCommitHash(): Computation<String> {
        val response = get("$baseRepositoryUrl/git/refs/heads/${repository.branch}")
        return mapResponse(response, this::extractRefSha)
    }

    fun postNewTree(baseRef: String, contentSha: Pair<String, String>): Computation<String> {
        val tree = Tree(baseRef, listOf(
                RefTreeObject(".travis.yml", TreeMode.FILE.mode, "blob", contentSha.second)
        ))
        val response = post("$baseRepositoryUrl/git/trees", mediaType, tree)
        return mapResponse(response, this::extractTreeHash)
    }

    fun postNewRef(refName: String, treeSha1: String): Computation<String> {
        val ref = RefCreationInput("refs/heads/$refName", treeSha1)
        val response = post("$baseRepositoryUrl/git/refs", mediaType, ref)
        return mapResponse(response, this::extractRefName)

    }

    fun postNewPullRequest(ref: String, botPullRequestSettings: BotPullRequestSettings): Computation<String> {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val pullRequest = PullRequestCreationInput(
                head = ref,
                title = botPullRequestSettings.title.replace("##date##", currentDateTime),
                body = botPullRequestSettings.message)
        val response = post("$baseRepositoryUrl/pulls", mediaType, pullRequest)
        return mapResponse(response, this::extractHtmlUrl)
    }

    fun postNewCommit(treeHash: String, baseHash: String, commitMessage: String): Computation<String> {
        val jsonMedia = mediaType
        val ref = CommitCreationInput(commitMessage, treeHash, listOf(baseHash))
        val response = post("$baseRepositoryUrl/git/commits", jsonMedia, ref)
        return mapResponse(response, this::extractCommitResponse)
    }

    private fun get(uri: String): Response {
        return httpClient.newCall(builderAt(uri).build()).execute()
    }

    private fun <T> post(uri: String, mediaType: MediaType, content: T): Response {
        val blob = gson.toJson(content)
        val rawAuthorization = "${repository.username}:${repository.authToken}"
        val requestBuilder = builderAt(uri).post(RequestBody.create(mediaType, blob))
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(rawAuthorization.toByteArray()))
                .build()
        return httpClient.newCall(requestBuilder).execute()
    }

    private fun builderAt(uri: String) = Request.Builder().url(uri)

    private fun <T> mapResponse(response: Response, fn: (Response) -> Computation<T>): Computation<T> {
        return if (!response.isSuccessful) {
            handleGithubErrors(response)
        } else {
            tryMap(fn, response)
        }
    }

    private fun <T> tryMap(fn: (Response) -> Computation<T>, response: Response): Computation<T> {
        return try {
            fn(response)
        } catch (e: JsonSyntaxException) {
            Failure(1001, e.message!!)
        }
    }

    private fun extractYaml(response: Response): Success<String> =
            Success(decodeBase64(removeNewlines(gson.fromJson<Map<String, String>>(response.body()!!.charStream(), Map::class.java)["content"]!!)))

    private fun extractBlobResponse(response: Response) =
            Success(gson.fromJson(response.body()!!.charStream(), BlobResponse::class.java).sha)

    private fun extractCommitResponse(response: Response) =
            Success(gson.fromJson(response.body()!!.charStream(), CommitResponse::class.java).sha)

    private fun extractRefSha(response: Response) =
            Success(gson.fromJson(response.body()!!.charStream(), RefResponse::class.java).`object`.sha)

    private fun extractTreeHash(response: Response) =
    // TODO: assert that only .travis.yml is listed as file and its SHA-1 matches
            Success(gson.fromJson(response.body()!!.charStream(), TreeResponse::class.java).sha)

    private fun extractRefName(response: Response) =
            Success(gson.fromJson(response.body()!!.charStream(), RefResponse::class.java).ref)

    private fun extractHtmlUrl(response: Response) =
            Success(gson.fromJson(response.body()!!.charStream(), PullRequestResponse::class.java).html_url)

    private fun <T> handleGithubErrors(response: Response): Failure<T> {
        return when (response.code()) {
            in 400..499 -> Failure(response.code(), gson.fromJson<Map<String, String>>(response.body()!!.charStream(), Map::class.java)["message"]!!)
            in 500..599 -> Failure(response.code(), "Unreachable $baseUri")
            else -> {
                Failure(response.code(), "Unexpected error")
            }
        }
    }

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