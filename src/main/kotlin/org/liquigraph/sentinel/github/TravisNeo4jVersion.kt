package org.liquigraph.sentinel.github

data class TravisNeo4jVersion(val version: SemanticVersion?, val inDockerStore: Boolean = false) {
    constructor(version: String, inDockerStore: Boolean) : this(SemanticVersion.parseEntire(version), inDockerStore)
}