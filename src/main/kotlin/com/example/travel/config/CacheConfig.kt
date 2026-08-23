package com.example.travel.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
class CacheConfig {

    companion object {
        const val PLACE_DETAILS = "placeDetails"
    }

    @Bean
    fun cacheManager(): CaffeineCacheManager =
        CaffeineCacheManager(PLACE_DETAILS).apply {
            setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .maximumSize(10_000)
                    .recordStats()
            )
        }
}