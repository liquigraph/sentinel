package org.liquigraph.sentinel

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.liquigraph.sentinel.github.LiquigraphService
import org.liquigraph.sentinel.github.Neo4jVersion
import org.liquigraph.sentinel.mavencentral.MavenCentralService
import org.liquigraph.sentinel.model.Failure
import org.liquigraph.sentinel.model.MavenCentralArtifact
import org.liquigraph.sentinel.model.Success
import java.io.PrintStream
import java.nio.file.Files

class SentinelRunnerTest {

    lateinit var previousOut: PrintStream
    lateinit var previousErr: PrintStream
    lateinit var out: CapturedOutputStream
    lateinit var err: CapturedOutputStream

    val liquigraphService = mock<LiquigraphService>()
    val mavenCentralService = mock<MavenCentralService>()
    val runner = SentinelRunner(liquigraphService, mavenCentralService)

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
        val travisVersions = listOf(Neo4jVersion("3.2.1", inDockerStore = true))
        val mavenArtifacts = listOf(MavenCentralArtifact("org.neo4j", "neo4j", "3.2.9", "jar", listOf(".jar")))
        whenever(liquigraphService.getNeo4jVersions()).thenReturn(
                Success(travisVersions))
        whenever(mavenCentralService.getNeo4jArtifacts()).thenReturn(
                Success(mavenArtifacts))

        runner.run()

        assertThat(out.buffer).contains(travisVersions.toString())
        assertThat(out.buffer).contains(mavenArtifacts.toString())
        assertThat(err.buffer).isEmpty()
    }

    @Test
    fun `reports Liquigraph service error`() {
        val error = Failure<List<Neo4jVersion>>(42, "I hear you had an error...?")
        whenever(liquigraphService.getNeo4jVersions()).thenReturn(error)

        runner.run()

        assertThat(err.buffer).contains(error.toString())
        assertThat(out.buffer).isEmpty()
    }

    @Test
    fun `reports Maven Central service error`() {
        val travisVersions = listOf(Neo4jVersion("3.2.1", inDockerStore = true))
        val error = Failure<List<MavenCentralArtifact>>(42, "I hear you had an error...?")
        whenever(liquigraphService.getNeo4jVersions()).thenReturn(
                Success(travisVersions))
        whenever(mavenCentralService.getNeo4jArtifacts()).thenReturn(error)

        runner.run()

        assertThat(err.buffer).contains(error.toString())
        assertThat(out.buffer).contains(travisVersions.toString())
    }


}

class CapturedOutputStream : PrintStream(Files.createTempFile("sentinel", "test").toFile()) {
    val buffer: MutableList<String> = mutableListOf()

    override fun println(instance: Any?) {
        buffer.add(instance.toString())
    }

}
