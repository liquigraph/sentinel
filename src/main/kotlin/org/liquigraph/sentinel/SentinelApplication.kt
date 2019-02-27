package org.liquigraph.sentinel

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

@SpringBootApplication
@EnableConfigurationProperties(WatchedCoordinates::class)
@PropertySource("classpath:watched-artifact.properties")
class SentinelApplication {

    @Bean
    fun yamlParser(): Yaml {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        val representer = Representer()
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
