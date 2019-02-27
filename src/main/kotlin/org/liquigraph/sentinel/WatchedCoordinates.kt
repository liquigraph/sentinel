package org.liquigraph.sentinel

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.PropertySource

@ConfigurationProperties("artifact")
class WatchedCoordinates {

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