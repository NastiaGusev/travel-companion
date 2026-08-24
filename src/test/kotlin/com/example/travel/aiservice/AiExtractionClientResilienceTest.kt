package com.example.travel.aiservice

import com.example.travel.client.aiService.AiExtractionClient
import com.example.travel.support.IntegrationTestBase
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.HttpServerErrorException

/**
 * Resilience4j retry + circuit breaker on the PROXIED AiExtractionClient bean, mirroring
 * PlacesResilienceTest. No @SpringBootTest redeclared — inherited from IntegrationTestBase.
 */
class AiExtractionClientResilienceTest : IntegrationTestBase() {

    @Autowired
    lateinit var aiExtractionClient: AiExtractionClient

    @Autowired
    lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    private val breaker: CircuitBreaker
        get() = circuitBreakerRegistry.circuitBreaker("aiService")

    companion object {
        private val wireMock = WireMockServer(options().dynamicPort()).apply { start() }

        @JvmStatic
        @AfterAll
        fun stopWireMock() = wireMock.stop()

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("ai.service.base-url") { wireMock.baseUrl() }

            // Dynamic props REPLACE the instance config, so it's restated here (shrunk speed/threshold
            // only) to mirror application.yml — same pattern as PlacesResilienceTest.
            registry.add("resilience4j.retry.instances.aiService.max-attempts") { "3" }
            registry.add("resilience4j.retry.instances.aiService.wait-duration") { "10ms" }
            registry.add("resilience4j.retry.instances.aiService.retry-exceptions[0]") {
                "org.springframework.web.client.HttpServerErrorException"
            }
            registry.add("resilience4j.retry.instances.aiService.retry-exceptions[1]") {
                "org.springframework.web.client.ResourceAccessException"
            }

            registry.add("resilience4j.circuitbreaker.instances.aiService.sliding-window-type") { "COUNT_BASED" }
            registry.add("resilience4j.circuitbreaker.instances.aiService.sliding-window-size") { "6" }
            registry.add("resilience4j.circuitbreaker.instances.aiService.minimum-number-of-calls") { "6" }
            registry.add("resilience4j.circuitbreaker.instances.aiService.failure-rate-threshold") { "50" }
            registry.add("resilience4j.circuitbreaker.instances.aiService.wait-duration-in-open-state") { "100ms" }
        }
    }

    @BeforeEach
    fun reset() {
        wireMock.resetAll()
        breaker.reset()
    }

    private val extract get() = post(urlPathEqualTo("/extract"))

    @Test
    fun `extract retries transient 5xx up to max-attempts`() {
        wireMock.stubFor(extract.willReturn(aResponse().withStatus(500)))

        assertThrows<HttpServerErrorException> { aiExtractionClient.extract("text") }

        wireMock.verify(3, postRequestedFor(urlPathEqualTo("/extract")))
    }

    @Test
    fun `breaker opens on repeated failure then fast-fails without hitting the wire`() {
        wireMock.stubFor(extract.willReturn(aResponse().withStatus(500)))

        repeat(2) {
            assertThrows<HttpServerErrorException> { aiExtractionClient.extract("text") }
        }
        assertThat(breaker.state).isEqualTo(CircuitBreaker.State.OPEN)

        val before = wireMock.findAll(postRequestedFor(urlPathEqualTo("/extract"))).size
        assertThrows<CallNotPermittedException> { aiExtractionClient.extract("text") }
        val after = wireMock.findAll(postRequestedFor(urlPathEqualTo("/extract"))).size

        assertThat(after).isEqualTo(before)
    }
}
