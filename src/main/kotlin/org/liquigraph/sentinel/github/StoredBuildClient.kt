package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.configuration.BotPullRequestSettings
import org.liquigraph.sentinel.configuration.WatchedGithubRepository
import org.liquigraph.sentinel.effects.CallError
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class StoredBuildClient(private final val repository: WatchedGithubRepository,
                        @Value("\${githubApi.baseUri}") private final val baseUri: String) {

    private val webClient = WebClient.create("$baseUri/repos/${repository.organization}/${repository.repository}")

    @NonNull
    fun fetchBuildDefinition(): Mono<Result<String>> {
        return get("/contents/.travis.yml")
                .flatMap { errorOrMap(it, java.util.Map::class.java) }
                .map { result ->
                    result.map { decodeBase64(removeNewlines(it["content"]!! as String)) }
                }
    }

    @NonNull
    fun postTravisYamlBlob(yaml: String): Mono<Pair<String, String>> {
        val blob = BlobInput(Base64.getEncoder().encodeToString(yaml.toByteArray(StandardCharsets.UTF_8)), "base64")
        return postJson("/git/blobs", blob)
                .flatMap { it.bodyToMono(BlobResponse::class.java) }
                .map { Pair(yaml, it.sha) }
    }

    @NonNull
    fun getMostRecentCommitHash(): Mono<Result<String>> {
        return get("/contents/.travis.yml")
                .flatMap { errorOrMap(it, RefResponse::class.java) }
                .map { result -> result.map { it.`object`.sha } }
    }

    @NonNull
    fun postNewTree(baseRef: String, contentSha: Pair<String, String>): Mono<String> {
        val tree = Tree(baseRef, listOf(
                RefTreeObject(".travis.yml", TreeMode.FILE.mode, "blob", contentSha.second)
        ))
        return postJson("/git/trees", tree)
                .flatMap { it.bodyToMono(TreeResponse::class.java) }
                .map { it.sha }
    }

    @NonNull
    fun postNewRef(refName: String, treeSha1: String): Mono<String> {
        val ref = RefCreationInput("refs/heads/$refName", treeSha1)
        return postJson("/git/refs", ref)
                .flatMap { it.bodyToMono(RefResponse::class.java) }
                .map { it.ref }
    }

    @NonNull
    fun postNewCommit(treeHash: String, baseHash: String, commitMessage: String): Mono<String> {
        val commit = CommitCreationInput(commitMessage, treeHash, listOf(baseHash))
        return postJson("/git/commits", commit)
                .flatMap { it.bodyToMono(CommitResponse::class.java) }
                .map { it.sha }
    }

    @NonNull
    fun postNewPullRequest(ref: String, botPullRequestSettings: BotPullRequestSettings): Mono<String> {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val pullRequest = PullRequestCreationInput(
                head = ref,
                title = botPullRequestSettings.title.replace("##date##", currentDateTime),
                body = botPullRequestSettings.message)

        return postJson("/pulls", pullRequest)
                .flatMap { it.bodyToMono(PullRequestResponse::class.java) }
                .map { it.html_url }
    }

    private fun get(uri: String): Mono<ClientResponse> {
        return webClient.get().uri(uri)
                .header(ACCEPT, APPLICATION_JSON_UTF8_VALUE)
                .exchange()
    }

    private fun <T> errorOrMap(response: ClientResponse, type: Class<T>): Mono<Result<T>> {
        val statusCode = response.statusCode()
        return when {
            statusCode.is4xxClientError -> {
                response.bodyToMono(Map::class.java)
                        .map { it["message"] as String }
                        .map { Result.failure<T>(CallError(statusCode.value(), it)) }

            }
            statusCode.is5xxServerError -> Mono.error(CallError(statusCode.value(), "Unreachable $baseUri"))
            else -> {
                val result: Mono<Result<T>> = response.bodyToMono(type).map { Result.success(it) }
                result
            }
        }
    }


    private fun <T> postJson(uri: String, content: T): Mono<ClientResponse> {
        val rawAuthorization = "${repository.username}:${repository.authToken}"
        return webClient.post().uri(uri)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(rawAuthorization.toByteArray()))
                .header(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .body(BodyInserters.fromObject(content))
                .accept(APPLICATION_JSON_UTF8)
                .exchange()
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