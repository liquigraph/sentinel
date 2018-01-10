package org.liquigraph.sentinel

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.liquigraph.sentinel.github.SemanticVersion
import org.liquigraph.sentinel.github.SemanticVersionAdapter
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.yaml.snakeyaml.Yaml


@SpringBootApplication
class SentinelApplication {

    @Bean
    fun yamlParser(): Yaml {
        return Yaml()
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
    SpringApplication.run(SentinelApplication::class.java, *args)
}
