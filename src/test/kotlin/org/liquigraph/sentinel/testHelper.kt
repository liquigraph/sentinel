package org.liquigraph.sentinel

fun String.toVersion() = SemanticVersion.parseEntire(this)!!