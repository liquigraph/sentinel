package org.liquigraph.sentinel

import org.liquigraph.sentinel.model.MavenCentralArtifact
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

    val mavenCentralApiResponse = """
        {
   "responseHeader":{
      "status":0,
      "QTime":9,
      "params":{
         "q":"g:\"org.neo4j\" AND a:\"neo4j\"",
         "core":"gav",
         "indent":"off",
         "fl":"id,g,a,v,p,ec,timestamp,tags",
         "sort":"score desc,timestamp desc,g asc,a asc,v desc",
         "rows":"20",
         "wt":"json",
         "version":"2.2"
      }
   },
   "response":{
      "numFound":207,
      "start":0,
      "docs":[
         {
            "id":"org.neo4j:neo4j:3.4.0-alpha04",
            "g":"org.neo4j",
            "a":"neo4j",
            "v":"3.4.0-alpha04",
            "p":"jar",
            "timestamp":1514895313000,
            "ec":[
               ".pom"
            ],
            "tags":[
               "libraries",
               "dependency",
               "maven",
               "package",
               "containing",
               "most",
               "neo4j",
               "used",
               "intended",
               "meta"
            ]
         },
         {
            "id":"org.neo4j:neo4j:3.2.9",
            "g":"org.neo4j",
            "a":"neo4j",
            "v":"3.2.9",
            "p":"jar",
            "timestamp":1514888927000,
            "ec":[
               "-sources.jar",
               ".jar"
            ],
            "tags":[
               "libraries",
               "dependency",
               "maven",
               "package",
               "containing",
               "most",
               "neo4j",
               "used",
               "intended",
               "meta"
            ]
         },
         {
            "id":"org.neo4j:neo4j:2.3.12",
            "g":"org.neo4j",
            "a":"neo4j",
            "v":"2.3.12",
            "p":"jar",
            "timestamp":1513085191000,
            "ec":[
               ".jar",
               ".pom"
            ],
            "tags":[
               "libraries",
               "dependency",
               "maven",
               "package",
               "containing",
               "most",
               "neo4j",
               "used",
               "intended",
               "meta"
            ]
         }
      ]
   }
}
        """.trimIndent()

    val mavenCentralArtifacts = listOf(
            MavenCentralArtifact("org.neo4j", "neo4j", "3.4.0-alpha04", "jar", listOf(".pom")),
            MavenCentralArtifact("org.neo4j", "neo4j", "3.2.9", "jar", listOf("-sources.jar", ".jar")),
            MavenCentralArtifact("org.neo4j", "neo4j", "2.3.12", "jar", listOf(".jar", ".pom"))
    )
}
