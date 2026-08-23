package com.example.travel.places

import com.example.travel.client.googlePlaces.PlacesClient
import com.example.travel.exception.PlaceNotFoundException
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
import org.springframework.cache.CacheManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.HttpServerErrorException

/**
 * Resilience4j retry + circuit breaker on the PROXIED PlacesClient bean.
 * No @SpringBootTest here — inherited from IntegrationTestBase (re-declaring it resets the web
 * environment and breaks TestRestTemplate at context load).
 *
 * PlacesClient throws transport-native types (HttpServerErrorException for 5xx) and only
 * translates 404 to PlaceNotFoundException. The 503 wrap happens at GlobalExceptionHandler,
 * which this test bypasses by calling the client bean directly — so assertions are on raw types.
 */
class PlacesResilienceTest : IntegrationTestBase() {

    @Autowired
    lateinit var placesClient: PlacesClient

    @Autowired
    lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired lateinit var cacheManager: CacheManager

    private val breaker: CircuitBreaker
        get() = circuitBreakerRegistry.circuitBreaker("googlePlaces")

    companion object {
        private val wireMock = WireMockServer(options().dynamicPort()).apply { start() }

        @JvmStatic
        @AfterAll
        fun stopWireMock() = wireMock.stop()

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            // Base-url carries /v1 to match prod's PlacesClientConfig (client URIs are /places/...).
            registry.add("google.places.base-url") { wireMock.baseUrl() + "/v1" }
            registry.add("google.places.api-key") { "test-key" }

            // Dynamic props REPLACE the instance config, so the full failure model is restated
            // here to mirror application.yml — only the speed/threshold knobs are shrunk.
            registry.add("resilience4j.retry.instances.googlePlaces.max-attempts") { "3" }
            registry.add("resilience4j.retry.instances.googlePlaces.wait-duration") { "10ms" }
            registry.add("resilience4j.retry.instances.googlePlaces.retry-exceptions[0]") {
                "org.springframework.web.client.HttpServerErrorException"
            }
            registry.add("resilience4j.retry.instances.googlePlaces.retry-exceptions[1]") {
                "org.springframework.web.client.ResourceAccessException"
            }
            registry.add("resilience4j.retry.instances.googlePlaces.ignore-exceptions[0]") {
                "com.example.travel.exception.PlaceNotFoundException"
            }

            registry.add("resilience4j.circuitbreaker.instances.googlePlaces.sliding-window-type") { "COUNT_BASED" }
            registry.add("resilience4j.circuitbreaker.instances.googlePlaces.sliding-window-size") { "6" }
            registry.add("resilience4j.circuitbreaker.instances.googlePlaces.minimum-number-of-calls") { "6" }
            registry.add("resilience4j.circuitbreaker.instances.googlePlaces.failure-rate-threshold") { "50" }
            registry.add("resilience4j.circuitbreaker.instances.googlePlaces.wait-duration-in-open-state") { "100ms" }
            registry.add("resilience4j.circuitbreaker.instances.googlePlaces.ignore-exceptions[0]") {
                "com.example.travel.exception.PlaceNotFoundException"
            }
        }
    }

    @BeforeEach
    fun reset() {
        wireMock.resetAll()
        breaker.reset()
        cacheManager.getCache("placeDetails")?.clear()
    }

    private val anyPlaceDetails get() = get(urlPathMatching("/v1/places/[^:].*"))
    private val autocomplete get() = post(urlPathMatching("/v1/places:autocomplete"))

    @Test
    fun `details retries transient 5xx up to max-attempts`() {
        wireMock.stubFor(anyPlaceDetails.willReturn(aResponse().withStatus(500)))

        assertThrows<HttpServerErrorException> { placesClient.details("ChIJ_test") }

        // Retry sits outside the breaker: each of the 3 attempts reaches the wire.
        wireMock.verify(3, getRequestedFor(urlPathMatching("/v1/places/.*")))
    }

    @Test
    fun `breaker opens on repeated failure then fast-fails without hitting the wire`() {
        wireMock.stubFor(anyPlaceDetails.willReturn(aResponse().withStatus(500)))

        // 2 logical calls × 3 attempts = 6 recorded failures = minimum-number-of-calls → OPEN.
        repeat(2) {
            assertThrows<HttpServerErrorException> { placesClient.details("ChIJ_test") }
        }
        assertThat(breaker.state).isEqualTo(CircuitBreaker.State.OPEN)

        val before = wireMock.findAll(getRequestedFor(urlPathMatching("/v1/places/.*"))).size
        assertThrows<CallNotPermittedException> { placesClient.details("ChIJ_test") }
        val after = wireMock.findAll(getRequestedFor(urlPathMatching("/v1/places/.*"))).size

        assertThat(after).isEqualTo(before) // fast-fail: no extra traffic
    }

    @Test
    fun `not-found does not trip the breaker`() {
        wireMock.stubFor(anyPlaceDetails.willReturn(aResponse().withStatus(404)))

        repeat(10) {
            assertThrows<PlaceNotFoundException> { placesClient.details("ChIJ_missing") }
        }
        assertThat(breaker.state).isEqualTo(CircuitBreaker.State.CLOSED)
        // Not retried (ignore-exceptions) and not recorded: exactly one request per call.
        wireMock.verify(10, getRequestedFor(urlPathMatching("/v1/places/.*")))
    }

    @Test
    fun `autocomplete degrades to empty list via fallback on upstream failure`() {
        wireMock.stubFor(autocomplete.willReturn(aResponse().withStatus(500)))

        val results = placesClient.autocomplete("eiffel", biasLat = 22.3, biasLng = 22.4)

        assertThat(results).isEmpty()
    }

    @Test
    fun `details caches by placeId, second call does not hit the wire`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/places/ChIJcached"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"id":"ChIJcached","displayName":{"text":"Eiffel Tower"}}"""))
        )

        val first = placesClient.details("ChIJcached")
        val second = placesClient.details("ChIJcached")

        assertThat(second).isEqualTo(first)
        wireMock.verify(1, getRequestedFor(urlPathMatching("/v1/places/.*"))) // only ONE wire call
    }
}