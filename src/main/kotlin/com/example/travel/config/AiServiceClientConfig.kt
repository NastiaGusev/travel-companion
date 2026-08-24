package com.example.travel.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
class AiServiceClientConfig {
    @Bean
    fun aiServiceRestClient(
        @Value($$"${ai.service.base-url}") baseUrl: String,
    ): RestClient {
        // ai-service is plain HTTP (no TLS) both in docker-compose and in WireMock-backed tests.
        // The JDK HttpClient's default behavior opportunistically upgrades a POST-with-body to
        // HTTP/2 cleartext ("h2c") — an upgrade a server that only speaks HTTP/1.1 (uvicorn here,
        // WireMock in tests) doesn't honor, dropping the request. Pinning HTTP/1.1 avoids the
        // upgrade attempt entirely. Places is unaffected: it's HTTPS, where TLS+ALPN negotiates
        // h2 properly, so no cleartext upgrade is ever attempted.
        val httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .build()
    }
}