package com.example.travel.quickadd

import com.example.travel.support.*
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.LocalTime
import java.util.UUID

/**
 * Full HTTP-to-HTTP path: POST /quick-add -> ai-service (WireMock) -> Places (WireMock) -> saved
 * days/stops. One shared WireMock server stands in for both downstreams, distinguished by path
 * (/extract vs /v1/places/...), same trick PlacesResilienceTest uses for Places alone.
 */
class QuickAddIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var rest: TestRestTemplate

    companion object {
        private val wireMock = WireMockServer(options().dynamicPort()).apply { start() }

        @JvmStatic
        @AfterAll
        fun stopWireMock() = wireMock.stop()

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("ai.service.base-url") { wireMock.baseUrl() }
            registry.add("google.places.base-url") { wireMock.baseUrl() + "/v1" }
            registry.add("google.places.api-key") { "test-key" }
        }
    }

    @BeforeEach
    fun reset() = wireMock.resetAll()

    private fun quickAdd(token: String, tripId: Any, text: String) =
        rest.exchange(
            "/api/trips/$tripId/quick-add", HttpMethod.POST,
            HttpEntity(mapOf("text" to text), bearerHeaders(token)), Map::class.java,
        )

    private fun stubExtract(body: String) =
        wireMock.stubFor(post(urlPathEqualTo("/extract")).willReturn(okJson(body)))

    private fun stubPlaceFound(placeId: String, name: String) {
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/places:autocomplete"))
                .willReturn(okJson("""{"suggestions":[{"placePrediction":{"placeId":"$placeId","text":{"text":"$name"}}}]}"""))
        )
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/places/$placeId"))
                .willReturn(okJson("""{"id":"$placeId","displayName":{"text":"$name"},"location":{"latitude":1.0,"longitude":2.0}}"""))
        )
    }

    @Test
    fun `quick-add creates days and stops resolved against Places`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!

        stubExtract(
            """
            { "days": [
              { "position": 1, "stops": [
                { "searchQuery": "Tsukiji Market", "name": "Tsukiji Market", "timeHint": "12:30" }
              ] }
            ] }
            """.trimIndent()
        )
        stubPlaceFound("ChIJxyz", "Tsukiji Market")

        val response = quickAdd(token, tripId, "day one: lunch at Tsukiji 12:30")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val days = response.body?.get("days") as List<*>
        assertThat(days).hasSize(1)
        val stops = (days[0] as Map<*, *>)["stops"] as List<*>
        val stop = stops[0] as Map<*, *>
        assertThat(stop["title"]).isEqualTo("Tsukiji Market")
        assertThat(LocalTime.parse(stop["startTime"] as String)).isEqualTo(LocalTime.of(12, 30))
        assertThat((stop["place"] as Map<*, *>)["placeId"]).isEqualTo("ChIJxyz")

        // and it's actually persisted, not just echoed back
        val persistedDays = rest.exchange<List<Map<*, *>>>(
            "/api/trips/$tripId/days", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(token)),
        )
        assertThat(persistedDays.body).hasSize(1)
    }

    @Test
    fun `nothing extractable returns 200 with an empty day list`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        stubExtract("""{ "days": [] }""")

        val response = quickAdd(token, tripId, "asdf")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("days") as List<*>).isEmpty()
    }

    @Test
    fun `a place that fails to resolve still creates the stop, title-only`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        stubExtract(
            """{ "days": [ { "position": 1, "stops": [
                { "searchQuery": "Nowhere", "name": "Nowhere" }
            ] } ] }"""
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/places:autocomplete")).willReturn(okJson("""{"suggestions":[]}"""))
        )

        val response = quickAdd(token, tripId, "text")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val days = response.body?.get("days") as List<*>
        val stops = (days[0] as Map<*, *>)["stops"] as List<*>
        val stop = stops[0] as Map<*, *>
        assertThat(stop["title"]).isEqualTo("Nowhere")
        assertThat(stop["place"]).isNull()
    }

    @Test
    fun `an ai-service failure returns 503 with AI_SERVICE_UNAVAILABLE`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        wireMock.stubFor(post(urlPathEqualTo("/extract")).willReturn(aResponse().withStatus(500)))

        val response = rest.exchange(
            "/api/trips/$tripId/quick-add", HttpMethod.POST,
            HttpEntity(mapOf("text" to "text"), bearerHeaders(token)), String::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(response.body).contains("AI_SERVICE_UNAVAILABLE")
    }

    @Test
    fun `blank text returns 400`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!

        val response = quickAdd(token, tripId, "")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `quick-add on another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tripId = rest.createTrip(tokenA)["id"]!!
        val tokenB = rest.registerAndGetToken()

        val response = rest.exchange(
            "/api/trips/$tripId/quick-add", HttpMethod.POST,
            HttpEntity(mapOf("text" to "text"), bearerHeaders(tokenB)), String::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `quick-add without a token returns 401`() {
        val response = rest.postForEntity(
            "/api/trips/${UUID.randomUUID()}/quick-add",
            mapOf("text" to "text"),
            String::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
