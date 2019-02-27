package org.liquigraph.sentinel

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.liquigraph.sentinel.configuration.BotPullRequestSettings
import org.liquigraph.sentinel.configuration.WatchedArtifact
import org.liquigraph.sentinel.configuration.WatchedGithubRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.TypeDescription
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.introspector.PropertyUtils
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import java.util.*

@SpringBootApplication
@EnableConfigurationProperties(WatchedArtifact::class, WatchedGithubRepository::class, BotPullRequestSettings::class)
@PropertySource(value = ["watched-artifact.properties", "watched-repository.properties", "sentinel-pr.properties"], encoding = "UTF-8")
class SentinelApplication {

    @Bean
    fun yamlParser(): Yaml {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.width = 350
        val representer = object : Representer() {
            override fun representScalar(tag: Tag?, value: String?, style: DumperOptions.ScalarStyle?): Node {
                return super.representScalar(tag, if (value == "null") "" else value, style)
            }
        }
        representer.propertyUtils.isSkipMissingProperties = true
        return Yaml(representer, options)
    }

    @Bean
    fun httpClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Bean
    fun gson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(SemanticVersion::class.java, SemanticVersionAdapter())
                .create()
    }
}

fun main(args: Array<String>) {
    runApplication<SentinelApplication>(*args)
}
