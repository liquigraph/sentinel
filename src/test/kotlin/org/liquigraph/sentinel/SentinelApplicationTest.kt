package org.liquigraph.sentinel

import io.mockk.mockk
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
            return mockk(relaxed = true)
        }
    }

    @Test
    fun `context loads`() {
    }
}

