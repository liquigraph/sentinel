package org.liquigraph.sentinel.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("github")
class WatchedGithubRepository {
    lateinit var organization: String
    lateinit var repository: String
    lateinit var branch: String
    lateinit var username: String
    lateinit var authToken: String
}