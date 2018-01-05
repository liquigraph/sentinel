package org.liquigraph.sentinel

import okhttp3.OkHttpClient
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
}

fun main(args: Array<String>) {
    SpringApplication.run(SentinelApplication::class.java, *args)
}
