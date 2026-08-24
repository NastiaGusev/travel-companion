package com.example.travel.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
class PlacesClientConfig {
    @Bean
    fun placesRestClient(
        @Value($$"${google.places.base-url}") baseUrl: String,
    ): RestClient {
        val httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .build()
    }
}