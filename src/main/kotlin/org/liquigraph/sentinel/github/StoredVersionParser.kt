package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.effects.AggregatedException
import org.liquigraph.sentinel.effects.flatMap
import org.liquigraph.sentinel.effects.partition
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import java.lang.IllegalArgumentException

@Component
class StoredVersionParser(private val yaml: Yaml) {

    fun parse(body: String): Result<List<StoredVersion>> {
        return parseBody(body)
                .map { it.env.matrix }
                .flatMap { extractVersions(it) }
    }

    private fun parseBody(body: String): Result<TravisBuildDefinition> {
        return try {
            Result.success(yaml.loadAs(body, TravisBuildDefinition::class.java))
        } catch (e: YAMLException) {
            Result.failure(e)
        }
    }

    private fun extractVersions(matrix: List<String>): Result<List<StoredVersion>> {
        val (failures, successes) = matrix.mapIndexed { index, row -> parseRow(index, row) }.partition()

        return if (failures.isNotEmpty()) {
            Result.failure(AggregatedException(failures.map { it.exceptionOrNull()!! }))
        } else {
            Result.success(successes.map { it.getOrNull()!! })
        }
    }

    private fun parseRow(index: Int, row: String): Result<StoredVersion> {
        val pairs = row.split(" ")
                .map {
                    val regex = Regex("([a-zA-Z_]*)=(.*)")
                    val groups = regex.matchEntire(it)!!.groups
                    Pair(groups[1]!!.value, groups[2]!!.value)
                }

        val neoVersion = pairs.firstOrNull { it.first == "NEO_VERSION" }
                ?: return Result.failure(IllegalArgumentException("Missing 'NEO_VERSION' field at index $index"))

        val withDocker = pairs.firstOrNull { it.first == "WITH_DOCKER" }
        return Result.success(StoredVersion(neoVersion.second, withDocker?.second?.toBoolean() ?: false))
    }

}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NoArgConstructor

@NoArgConstructor
data class TravisBuildDefinition(var env: TravisEnvironment)

@NoArgConstructor
data class TravisEnvironment(var matrix: List<String>)