package com.example.travel.places

import com.example.travel.client.googlePlaces.PlacesClient
import com.example.travel.exception.PlaceNotFoundException
import com.example.travel.exception.PlacesUnavailableException
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

class PlacesClientMappingTest {

    @RegisterExtension
    val wireMock: WireMockExtension = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build()

    private lateinit var client: PlacesClient

    @BeforeEach
    fun setUp() {
        val jdkClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()

        val restClient = RestClient.builder()
            .baseUrl(wireMock.baseUrl())
            .requestFactory(JdkClientHttpRequestFactory(jdkClient))
            .build()

        client = PlacesClient(restClient, "test-key")
    }

    @Test
    fun `details maps Google response onto DetailedPlace`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/places/ChIJxyz"))
                .willReturn(
                    okJson(
                        """
                    {
                      "id": "ChIJxyz",
                      "displayName": { "text": "Paphos Castle" },
                      "location": { "latitude": 34.7533, "longitude": 32.4076 },
                      "formattedAddress": "Paphos Harbour, Cyprus",
                      "types": ["tourist_attraction"]
                    }
                """.trimIndent()
                    )
                )
        )

        val place = client.details("ChIJxyz")

        assertThat(place.placeId).isEqualTo("ChIJxyz")
        assertThat(place.name).isEqualTo("Paphos Castle")
        assertThat(place.latitude).isEqualTo(34.7533)
        assertThat(place.category).isEqualTo("tourist_attraction")

        wireMock.verify(
            getRequestedFor(urlPathEqualTo("/places/ChIJxyz"))
                .withHeader("X-Goog-FieldMask", containing("displayName"))
        )
    }

    @Test
    fun `autocomplete maps suggestions to PlaceSearchResultDto`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/places:autocomplete"))
                .willReturn(okJson("""
                {
                  "suggestions": [
                    {
                      "placePrediction": {
                        "placeId": "ChIJxyz",
                        "text": { "text": "Paphos Castle, Cyprus" }
                      }
                    }
                  ]
                }
            """.trimIndent()))
        )

        val results = client.autocomplete(
            query = "paph",
            biasLat = 34.77,
            biasLng = 32.42
        )

        assertThat(results).hasSize(1)
        assertThat(results[0].placeId).isEqualTo("ChIJxyz")
        assertThat(results[0].description).isEqualTo("Paphos Castle, Cyprus")

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/places:autocomplete"))
                .withRequestBody(matchingJsonPath("$.locationBias.circle.center.latitude"))
                .withRequestBody(matchingJsonPath("$.locationBias.circle.radius"))
                .withRequestBody(matchingJsonPath("$.input", equalTo("paph")))
        )
    }

    @Test
    fun `autocomplete returns empty list when no suggestions`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/places:autocomplete"))
                .willReturn(okJson("{}"))
        )

        assertThat(client.autocomplete(query = "zzz", biasLat = 34.77, biasLng = 32.42)).isEmpty()
    }

    @Test
    fun `details throws PlaceNotFoundException on 404`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/places/ChIJmissing"))
                .willReturn(aResponse().withStatus(404)
                    .withBody("""{"error":{"code":404,"status":"NOT_FOUND"}}"""))
        )

        assertThatThrownBy { client.details("ChIJmissing") }
            .isInstanceOf(PlaceNotFoundException::class.java)
    }

    @Test
    fun `details throws PlacesUnavailableException on server error`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/places/ChIJboom"))
                .willReturn(aResponse().withStatus(500))
        )

        assertThatThrownBy { client.details("ChIJboom") }
            .isInstanceOf(PlacesUnavailableException::class.java)
    }

    @Test
    fun `details maps a place with no types or address`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/places/ChIJbare"))
                .willReturn(okJson("""
                {
                  "id": "ChIJbare",
                  "displayName": { "text": "Nameless Spot" },
                  "location": { "latitude": 34.0, "longitude": 32.0 }
                }
            """.trimIndent()))
        )

        val place = client.details("ChIJbare")

        assertThat(place.placeId).isEqualTo("ChIJbare")
        assertThat(place.category).isNull()
    }
}