package org.liquigraph.sentinel.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("sentinel.pr")
class BotPullRequestSettings {
    lateinit var title: String
    lateinit var message: String
    lateinit var branchName: String
}