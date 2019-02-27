package org.liquigraph.sentinel

import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class SentinelApplicationTest {

    @Configuration
    class Config {
        @Bean
        fun mockRunner(): SentinelRunner {
            return mock()
        }
    }

    @Test
    fun `context loads`() {
    }
}

