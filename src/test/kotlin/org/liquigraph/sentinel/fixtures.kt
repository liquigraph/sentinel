package org.liquigraph.sentinel

import java.nio.charset.StandardCharsets
import java.util.*

object Fixtures {

    val travisYml = """
    |sudo: required
    |language: java
    |services:
    |  - docker
    |jdk:
    |  - oraclejdk8
    |os:
    |  - linux
    |env:
    |  matrix:
    |    - NEO_VERSION=3.0.11
    |      WITH_DOCKER=true
    |      EXTRA_PROFILES=-Pwith-neo4j-io
    |    - NEO_VERSION=3.1.7
    |      WITH_DOCKER=false
    |      EXTRA_PROFILES=-Pwith-neo4j-io
""".trimMargin()

    private val travisYmlBase64 = Base64.getEncoder().encodeToString(travisYml.toByteArray(StandardCharsets.UTF_8))

    val githubFileApiResponse = """{
        "name": ".travis.yml",
        "path": ".travis.yml",
        "sha": "45e87146451aa1f8cae0eb76297d563fc77776ad",
        "size": 1054,
        "url": "https://api.github.com/repos/liquigraph/liquigraph/contents/.travis.yml?ref=master",
        "html_url": "https://github.com/liquigraph/liquigraph/blob/master/.travis.yml",
        "git_url": "https://api.github.com/repos/liquigraph/liquigraph/git/blobs/45e87146451aa1f8cae0eb76297d563fc77776ad",
        "download_url": "https://raw.githubusercontent.com/liquigraph/liquigraph/master/.travis.yml",
        "type": "file",
        "content": "\n$travisYmlBase64\n",
        "encoding": "base64",
        "_links": {
            "self": "https://api.github.com/repos/liquigraph/liquigraph/contents/.travis.yml?ref=master",
            "git": "https://api.github.com/repos/liquigraph/liquigraph/git/blobs/45e87146451aa1f8cae0eb76297d563fc77776ad",
            "html": "https://github.com/liquigraph/liquigraph/blob/master/.travis.yml"
        }
    }
    """.trimIndent()
}
