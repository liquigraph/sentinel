package org.liquigraph.sentinel

import org.liquigraph.sentinel.mavencentral.MavenArtifact
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
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
            MavenArtifact("org.neo4j", "neo4j", "3.4.0-alpha04".toVersion(), "jar", listOf(".pom")),
            MavenArtifact("org.neo4j", "neo4j", "3.2.9".toVersion(), "jar", listOf("-sources.jar", ".jar")),
            MavenArtifact("org.neo4j", "neo4j", "2.3.12".toVersion(), "jar", listOf(".jar", ".pom"))
    )


    fun yamlParser(): Yaml {
        val representer = Representer()
        representer.propertyUtils.isSkipMissingProperties = true
        return Yaml(representer)
    }

    val dockerStubResponse = """
{"id":"5f0be9a7-f5d7-4974-9c0e-78c33484e79c","name":"neo4j","slug":"neo4j","type":"image","publisher":{"id":"docker","name":"Docker"},"created_at":"2016-06-09T20:42:23.465943Z","updated_at":"2018-01-30T20:26:44.174693Z","short_description":"Neo4j is a highly scalable, robust native graph database.","full_description":"# Supported tags and respective `Dockerfile` links\n\n-\t[`3.3.2`, `3.3`, `latest` (*3.3.2/community/Dockerfile*)](https://github.com/neo4j/docker-neo4j-publish/blob/d2ac73d32328f299d14aad08bb82e7daefe1e575/3.3.2/community/Dockerfile)\n-\t[`3.3.2-enterprise`, `3.3-enterprise`, `enterprise` (*3.3.2/enterprise/Dockerfile*)](https://github.com/neo4j/docker-neo4j-publish/blob/d2ac73d32328f299d14aad08bb82e7daefe1e575/3.3.2/enterprise/Dockerfile)\n-\t[`3.3.1` (*3.3.1/community/Dockerfile*)](https://github.com/neo4j/docker-neo4j-publish/blob/9a175bdb484967c609c5c369256b866a577f86b3/3.3.1/community/Dockerfile)\n-\t[`3.3.1-enterprise` (*3.3.1/enterprise/Dockerfile*)](https://github.com/neo4j/docker-neo4j-publish/blob/9a175bdb484967c609c5c369256b866a577f86b3/3.3.1/enterprise/Dockerfile)\n-\t[`3.3.0` (*3.3.0/community/Dockerfile*)](https://github.com/neo4j/docker-neo4j-publish/blob/aa31654ee8544cd544b369d2646cf372086f7b70/3.3.0/community/Dockerfile)\n-\t[`3.3.0-enterprise` (*3.3.0/enterprise/Dockerfile*)](https://github.com/neo4j/docker-neo4j-publish/blob/aa31654ee8544cd544b369d2646cf372086f7b70/3.3.0/enterprise/Dockerfile)\n-\t\n\n# Quick reference\n\n-\t**Where to get help**:  \n\t[Stack Overflow](http://stackoverflow.com/questions/tagged/neo4j)\n\n-\t**Where to file issues**:  \n\t[https://github.com/neo4j/docker-neo4j/issues](https://github.com/neo4j/docker-neo4j/issues)\n\n-\t**Maintained by**:  \n\t[Neo4j](https://github.com/neo4j/docker-neo4j)\n\n-\t**Supported architectures**: ([more info](https://github.com/docker-library/official-images#architectures-other-than-amd64))  \n\t[`amd64`](https://hub.docker.com/r/amd64/neo4j/)\n\n-\t**Published image artifact details**:  \n\t[repo-info repo's `repos/neo4j/` directory](https://github.com/docker-library/repo-info/blob/master/repos/neo4j) ([history](https://github.com/docker-library/repo-info/commits/master/repos/neo4j))  \n\t(image metadata, transfer size, etc)\n\n-\t**Image updates**:  \n\t[official-images PRs with label `library/neo4j`](https://github.com/docker-library/official-images/pulls?q=label%3Alibrary%2Fneo4j)  \n\t[official-images repo's `library/neo4j` file](https://github.com/docker-library/official-images/blob/master/library/neo4j) ([history](https://github.com/docker-library/official-images/commits/master/library/neo4j))\n\n-\t**Source of this description**:  \n\t[docs repo's `neo4j/` directory](https://github.com/docker-library/docs/tree/master/neo4j) ([history](https://github.com/docker-library/docs/commits/master/neo4j))\n\n-\t**Supported Docker versions**:  \n\t[the latest release](https://github.com/docker/docker-ce/releases/latest) (down to 1.6 on a best-effort basis)\n\n# What is Neo4j?\n\nNeo4j is a highly scalable, robust, native graph database. It is used in mission-critical apps by thousands of leading startups, enterprises, and governments around the world. You can learn more [here](http://neo4j.com/developer).\n\n![logo](https://raw.githubusercontent.com/docker-library/docs/2289fb3b561c63750032ac74ff65034c0e486072/neo4j/logo.png)\n\n# How to use this image\n\n## Start an instance of neo4j\n\nYou can start a Neo4j container like this:\n\n```console\ndocker run \\\n    --publish=7474:7474 --publish=7687:7687 \\\n    --volume=HOME/neo4j/data:/data \\\n    neo4j\n```\n\nwhich allows you to access neo4j through your browser at [http://localhost:7474](http://localhost:7474).\n\nThis binds two ports (`7474` and `7687`) for HTTP and Bolt access to the Neo4j API. A volume is bound to `/data` to allow the database to be persisted outside the container.\n\nBy default, this requires you to login with `neo4j/neo4j` and change the password. You can, for development purposes, disable authentication by passing `--env=NEO4J_AUTH=none` to docker run.\n\n## Note on version 2.3\n\nNeo4j 3.0 introduced several major user-facing changes, primarily the new binary Bolt protocol. This is not available in 2.3 and as such, there is no need to expose the `7687` port. Due to changes made to the structure of configuration files, several environment variables used to configure the image has changed as well. Please see the [2.x specific section in the manual](http://neo4j.com/developer/docker-23/) for further details.\n\nYou can start an instance of Neo4j 2.3 like this:\n\n```console\ndocker run \\\n    --publish=7474:7474 \\\n    --volume=HOME/neo4j/data:/data \\\n    neo4j:2.3\n```\n\n# Documentation\n\nFor more examples and complete documentation please go [here for 2.x](http://neo4j.com/developer/docker-23/) and [here for 3.x](http://neo4j.com/docs/operations-manual/current/deployment/single-instance/docker/).\n\n# License\n\nView [licensing information](https://neo4j.com/licensing) for the software contained in this image.\n\nAs with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).\n\nSome additional license information which was able to be auto-detected might be found in [the `repo-info` repository's `neo4j/` directory](https://github.com/docker-library/repo-info/tree/master/repos/neo4j).\n\nAs for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.","source":"library","popularity":11676467,"categories":[{"name":"database","label":"Databases"}],"operating_systems":[{"name":"linux","label":"Linux"}],"architectures":[{"name":"amd64","label":"x86-64"}],"links":null,"screenshots":[],"logo_url":{"large":"https://d1q6f0aelx0por.cloudfront.net/product-logos/2cd4f81a-e285-4ede-9d9f-99c2349bdd4b-neo4j.png"},"is_offline":false,"plans":[{"id":"cf33711b-7dea-45ff-a62f-750700499182","name":"Free Product Tier","description":"","instructions":"","download_attribute":"anonymous","eusa":"","eusa_type":"url","default_version":{"linux":"latest"},"versions":[{"os":"linux","architecture":"amd64","tags":[{"value":"latest","latest":true}]}],"operating_systems":[{"name":"linux","label":"Linux"}],"architectures":[{"name":"amd64","label":"x86-64"}],"certification_status":"not_certified","repositories":[{"namespace":"library","reponame":"neo4j"}]}]}
        """.trimIndent()
}
