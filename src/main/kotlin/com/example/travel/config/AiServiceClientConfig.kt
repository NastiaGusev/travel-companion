package com.example.travel.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AiServiceClientConfig {
    @Bean
    fun aiServiceRestClient(
        @Value($$"${ai.service.base-url}") baseUrl: String,
    ): RestClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .build()
}
