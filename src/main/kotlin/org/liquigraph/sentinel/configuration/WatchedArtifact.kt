package org.liquigraph.sentinel.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("artifact")
class WatchedArtifact {

    lateinit var name: String
    val maven = MavenCoordinates()
    val docker = DockerCoordinates()

    class MavenCoordinates {
        lateinit var groupId: String
        lateinit var artifactId: String
        lateinit var packaging: String
        lateinit var classifier: String
    }

    class DockerCoordinates {
        lateinit var image: String
    }
}