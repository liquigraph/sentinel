package org.liquigraph.sentinel

import org.liquigraph.sentinel.github.SemanticVersion

fun String.toVersion() = SemanticVersion.parseEntire(this)!!