package org.liquigraph.sentinel.github

import org.liquigraph.sentinel.SemanticVersion

data class StoredVersion(val version: SemanticVersion, val inDockerStore: Boolean = false) {
    constructor(version: String, inDockerStore: Boolean) : this(SemanticVersion.parseEntire(version)!!, inDockerStore)

    override fun toString(): String {
        return "$version | $inDockerStore"
    }
}