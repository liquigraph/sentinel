package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.model.Failure
import org.liquigraph.sentinel.model.Result
import org.liquigraph.sentinel.model.Success
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException

@Component
class Neo4jVersionParser(val yaml: Yaml) {

    fun parse(body: String): Result<List<Neo4jVersion>> {
        val payload = parseBody(body)
        return if (!payload.isSuccessful()) {
            val error = payload as Failure<Map<String, Any>?>
            Failure(error.code, error.message)
        }
        else {
            val buildMatrix = readBuildMatrix((payload as Success<Map<String, Any>?>).getContent())
            when (buildMatrix) {
                is Failure -> Failure(buildMatrix.code, buildMatrix.message)
                is Success<List<String>> -> {
                    return extractVersions(buildMatrix.getContent())
                }
            }
        }
    }

    private fun parseBody(body: String): Result<Map<String, Any>?> {
        return try {
            Success(yaml.load<Map<String, Any>>(body))
        }
        catch (e: YAMLException) {
            Failure(1000, e.message ?: "Invalid YAML")
        }
    }

    private fun readBuildMatrix(body:Map<String, Any>?): Result<List<String>> {
        val env = if (body == null) null else body["env"] as Map<String, List<String>>?
        return when {
            env == null -> Failure(1001, "Could not find 'env' field")
            env["matrix"] == null -> Failure(1001, "Could not find 'matrix' field")
            else -> Success(env["matrix"]!!)
        }
    }

    private fun extractVersions(matrix: List<String>): Result<List<Neo4jVersion>> {
        val versions = matrix.mapIndexed { index, row ->
            parseRow(index, row)
        }
        val errors = versions.filter { !it.isSuccessful() }
        return if (errors.isNotEmpty()) {
            val messages = errors.joinToString("\n") { (it as Failure).message }
            Failure((errors.first() as Failure).code, messages)
        } else {
            Success(versions.map { it.getContent() })
        }
    }

    private fun parseRow(index: Int, row: String): Result<Neo4jVersion> {
        val pairs = row.split(" ")
                .map {
                    val regex = Regex("([a-zA-Z_]*)=(.*)")
                    val groups = regex.matchEntire(it)!!.groups
                    Pair(groups[1]!!.value, groups[2]!!.value)
                }

        val neoVersion = pairs.firstOrNull { it.first == "NEO_VERSION" }
        if (neoVersion == null) {
            return Failure(1002, "Missing 'NEO_VERSION' field at index $index")
        }
        val version = neoVersion.second // access
        val withDocker = pairs.firstOrNull { it.first == "WITH_DOCKER" }
        return Success(Neo4jVersion(version, withDocker?.second?.toBoolean() ?: false))
    }

}