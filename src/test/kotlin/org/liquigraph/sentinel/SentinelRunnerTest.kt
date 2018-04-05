package org.liquigraph.sentinel

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.liquigraph.sentinel.dockerstore.DockerStoreService
import org.liquigraph.sentinel.effects.Failure
import org.liquigraph.sentinel.effects.Success
import org.liquigraph.sentinel.github.*
import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import java.io.PrintStream
import java.nio.file.Files
import java.util.logging.LogManager

class SentinelRunnerTest {

    private lateinit var previousOut: PrintStream
    private lateinit var previousErr: PrintStream
    private lateinit var out: CapturedOutputStream
    private lateinit var err: CapturedOutputStream

    private val travisYamlService = mock<TravisYamlService>()
    private val mavenCentralService = mock<MavenCentralService>()
    private val dockerStoreService = mock<DockerStoreService>()
    private val liquigraphService = LiquigraphService()
    private val runner = SentinelRunner(travisYamlService, mavenCentralService, liquigraphService, dockerStoreService)

    init {
        LogManager.getLogManager().reset()
    }

    @Before
    fun prepare() {
        previousOut = System.out
        previousErr = System.err
        out = CapturedOutputStream()
        err = CapturedOutputStream()
        System.setOut(out)
        System.setErr(err)
    }

    @After
    fun cleanUp() {
        System.setOut(previousOut)
        System.setErr(previousErr)
    }

    @Test
    fun `runs runs ruuuuuns`() {
        val travisVersions = listOf(TravisNeo4jVersion("3.2.1", inDockerStore = true))
        val mavenArtifacts = listOf(MavenArtifact("org.neo4j", "neo4j", "3.2.9".toVersion(), "jar", listOf(".jar")))
        val dockerizedVersions = setOf("3.2.9".toVersion())
        whenever(travisYamlService.getNeo4jVersions()).thenReturn(
                Success(travisVersions))
        whenever(mavenCentralService.getNeo4jArtifacts()).thenReturn(
                Success(mavenArtifacts))
        whenever(dockerStoreService.fetchDockerizedNeo4jVersions()).thenReturn(
                Success(dockerizedVersions))


        runner.run()

        assertThat(out.buffer).contains(travisVersions.joinLines())
        assertThat(out.buffer).contains(mavenArtifacts.joinLines())
        assertThat(out.buffer).contains(dockerizedVersions.joinLines())
        assertThat(err.buffer).isEmpty()
    }

    @Test
    fun `reports Liquigraph service error`() {
        val error = Failure<List<TravisNeo4jVersion>>(42, "I hear you had an error...?")
        whenever(travisYamlService.getNeo4jVersions()).thenReturn(error)

        runner.run()

        assertThat(err.buffer).contains(error.toString())
        assertThat(out.buffer).isEmpty()
    }

    @Test
    fun `reports Maven Central service error`() {
        val travisVersions = listOf(TravisNeo4jVersion("3.2.1", inDockerStore = true))
        val error = Failure<List<MavenArtifact>>(42, "I hear you had an error...?")
        whenever(travisYamlService.getNeo4jVersions()).thenReturn(
                Success(travisVersions))
        whenever(mavenCentralService.getNeo4jArtifacts()).thenReturn(error)

        runner.run()

        assertThat(err.buffer).contains(error.toString())
        assertThat(out.buffer).contains(travisVersions.joinLines())
    }

    @Test
    fun `reports Docker Store service error`() {
        val travisVersions = listOf(TravisNeo4jVersion("3.2.1", inDockerStore = true))
        val mavenArtifacts = listOf(MavenArtifact("org.neo4j", "neo4j", "3.2.9".toVersion(), "jar", listOf(".jar")))
        val error = Failure<Set<SemanticVersion>>(42, "I hear you had an error...?")
        whenever(travisYamlService.getNeo4jVersions()).thenReturn(
                Success(travisVersions))
        whenever(mavenCentralService.getNeo4jArtifacts()).thenReturn(
                Success(mavenArtifacts))
        whenever(dockerStoreService.fetchDockerizedNeo4jVersions()).thenReturn(error)

        runner.run()

        assertThat(err.buffer).contains(error.toString())
        assertThat(out.buffer).contains(travisVersions.joinLines())
    }

    @Test
    fun `reports the version changes`() {
        val travisVersions = listOf(TravisNeo4jVersion("3.2.1", inDockerStore = true))
        val mavenArtifacts = listOf(MavenArtifact("org.neo4j", "neo4j", "3.2.9".toVersion(), "jar", listOf(".jar")))
        whenever(travisYamlService.getNeo4jVersions()).thenReturn(
                Success(travisVersions))
        whenever(mavenCentralService.getNeo4jArtifacts()).thenReturn(
                Success(mavenArtifacts))
        whenever(dockerStoreService.fetchDockerizedNeo4jVersions()).thenReturn(Success(setOf()))

        runner.run()

        assertThat(out.buffer).contains(listOf(Addition("3.2.9".toVersion(), false)).joinLines())
        assertThat(err.buffer).isEmpty()
    }
}

class CapturedOutputStream : PrintStream(Files.createTempFile("sentinel", "test").toFile()) {
    val buffer: MutableList<String> = mutableListOf()

    override fun println(instance: Any?) {
        buffer.add(instance.toString())
    }

}
