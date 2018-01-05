package org.liquigraph.sentinel.github

import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml

@Component
class Neo4jVersionParser(val yaml: Yaml) {

    fun parse(body: String): List<Neo4jVersion> {
        val payload = yaml.load<Map<String, Any>>(body)
        val matrix = (payload["env"] as Map<String, List<String>>)["matrix"]!!
        return matrix.map {
            parseRow(it)
        }
    }

    private fun parseRow(row: String): Neo4jVersion {
        val pairs = row.split(" ")
                .map {
                    val regex = Regex("([a-zA-Z_]*)=(.*)")
                    val groups = regex.matchEntire(it)!!.groups
                    Pair(groups[1]!!.value, groups[2]!!.value)
                }
        val version = pairs.first { it.first == "NEO_VERSION" }.second
        val withDocker = pairs.firstOrNull() { it.first == "WITH_DOCKER" }
        return Neo4jVersion(version, withDocker?.second?.toBoolean() ?: false)
    }

}