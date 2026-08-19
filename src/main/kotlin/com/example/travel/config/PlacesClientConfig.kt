package com.example.travel.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class PlacesClientConfig {
    @Bean
    fun placesRestClient(
        @Value($$"${google.places.base-url}") baseUrl: String,
    ): RestClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .build()
}