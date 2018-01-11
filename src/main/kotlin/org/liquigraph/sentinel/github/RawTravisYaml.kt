package org.liquigraph.sentinel.github

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NoArgConstructor

@NoArgConstructor data class RawTravisYaml(var env: RawTravisEnv)
@NoArgConstructor data class RawTravisEnv(var matrix: List<String>)
