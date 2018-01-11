package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Result
import org.liquigraph.sentinel.effects.Success
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException

@Component
class TravisNeo4jVersionParser(val yaml: Yaml) {

    fun parse(body: String): Result<List<TravisNeo4jVersion>> {
        return parseBody(body)
                .flatMap { readBuildMatrix(it) }
                .flatMap { extractVersions(it) }
    }

    private fun parseBody(body: String): Result<Map<String, Any>?> {
        return try {
            Success(yaml.load<Map<String, Any>>(body))
        } catch (e: YAMLException) {
            Failure(1000, e.message ?: "Invalid YAML")
        }
    }

    private fun readBuildMatrix(body: Map<String, Any>?): Result<List<String>> {
        val env = if (body == null) null else body["env"] as Map<String, List<String>>?
        return when {
            env == null -> Failure(1001, "Could not find 'env' field")
            env["matrix"] == null -> Failure(1001, "Could not find 'matrix' field")
            else -> Success(env["matrix"]!!)
        }
    }

    private fun extractVersions(matrix: List<String>): Result<List<TravisNeo4jVersion>> {
        val versions: List<Result<TravisNeo4jVersion>> = matrix.mapIndexed { index, row -> parseRow(index, row) }
        val (failures, successes) = partition(versions)

        return if (failures.isNotEmpty()) {
            val messages = failures.joinToString("\n") { it.message }
            Failure(failures.first().code, messages)
        } else {
            Success(successes.map { it.content })
        }
    }

    private fun <T> partition(input: List<Result<T>>) =
            input.fold(Pair(emptyList<Failure<T>>(), emptyList<Success<T>>())) { (failures, successes), element ->
                when (element) {
                    is Failure -> Pair(failures + element, successes)
                    is Success -> Pair(failures, successes + element)
                }
            }

    private fun parseRow(index: Int, row: String): Result<TravisNeo4jVersion> {
        val pairs = row.split(" ")
                .map {
                    val regex = Regex("([a-zA-Z_]*)=(.*)")
                    val groups = regex.matchEntire(it)!!.groups
                    Pair(groups[1]!!.value, groups[2]!!.value)
                }

        val neoVersion = pairs.firstOrNull { it.first == "NEO_VERSION" }
                ?: return Failure(1002, "Missing 'NEO_VERSION' field at index $index")

        val withDocker = pairs.firstOrNull { it.first == "WITH_DOCKER" }
        return Success(TravisNeo4jVersion(neoVersion.second, withDocker?.second?.toBoolean() ?: false))
    }

}