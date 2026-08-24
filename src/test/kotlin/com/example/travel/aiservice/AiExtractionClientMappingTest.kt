package com.example.travel.aiservice

import com.example.travel.client.aiService.AiExtractionClient
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

/**
 * Plain JUnit, no Spring context — covers HTTP wiring and JSON mapping against the ai-service
 * contract (see ai-service/README.md), same split as PlacesClientMappingTest vs
 * PlacesResilienceTest: mapping here, retry/breaker under Spring in AiExtractionClientResilienceTest.
 */
class AiExtractionClientMappingTest {

    @RegisterExtension
    val wireMock: WireMockExtension = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build()

    private lateinit var client: AiExtractionClient

    @BeforeEach
    fun setUp() {
        // HTTP/1.1 pin: the JDK client's default h2c upgrade attempt against WireMock's cleartext
        // endpoint drops POST bodies (same gotcha hit on PlacesClientMappingTest).
        val jdkClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()

        val restClient = RestClient.builder()
            .baseUrl(wireMock.baseUrl())
            .requestFactory(JdkClientHttpRequestFactory(jdkClient))
            .build()

        client = AiExtractionClient(restClient)
    }

    @Test
    fun `extract maps a multi-day response`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/extract"))
                .willReturn(
                    okJson(
                        """
                        {
                          "days": [
                            { "position": 1, "stops": [
                              { "searchQuery": "Tsukiji Market", "name": "Tsukiji Market", "timeHint": "12:30" }
                            ] },
                            { "position": 2, "stops": [
                              { "searchQuery": "Gonpachi", "name": "Gonpachi" }
                            ] }
                          ]
                        }
                        """.trimIndent()
                    )
                )
        )

        val result = client.extract("day one: lunch at Tsukiji 12:30. day two: dinner at Gonpachi")

        assertThat(result.days).hasSize(2)
        assertThat(result.days[0].stops[0].searchQuery).isEqualTo("Tsukiji Market")
        assertThat(result.days[0].stops[0].timeHint).isEqualTo("12:30")
        assertThat(result.days[1].stops[0].timeHint).isNull()

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/extract"))
                .withRequestBody(matchingJsonPath("$.text"))
        )
    }

    @Test
    fun `extract maps nothing-extractable to an empty day list`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/extract"))
                .willReturn(okJson("""{ "days": [] }"""))
        )

        assertThat(client.extract("asdf").days).isEmpty()
    }
}
