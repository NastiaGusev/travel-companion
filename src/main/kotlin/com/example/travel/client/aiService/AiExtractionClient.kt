package com.example.travel.client.aiService

import com.example.travel.client.aiService.dto.ExtractRequest
import com.example.travel.client.aiService.dto.ExtractResponse
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Thin client for the ai-service /extract endpoint (see ai-service/README.md for the contract).
 *
 * Deliberately NO fallback here, unlike PlacesClient.autocomplete: a quick-add is the user's
 * whole request, so degrading to an empty result on failure would silently do nothing. Instead
 * the raw failure (HttpServerErrorException / ResourceAccessException / CallNotPermittedException)
 * escapes to QuickAddService, which wraps it into AiServiceUnavailableException -> 503. Letting it
 * escape this method (rather than catching here) is what keeps @Retry/@CircuitBreaker working —
 * they only see exceptions escaping the annotated method, the same lesson learned on PlacesClient.
 */
@Component
class AiExtractionClient(
    private val aiServiceRestClient: RestClient,
) {
    @CircuitBreaker(name = "aiService")
    @Retry(name = "aiService")
    fun extract(text: String): ExtractResponse =
        aiServiceRestClient.post()
            .uri("/extract")
            .contentType(MediaType.APPLICATION_JSON)
            .body(ExtractRequest(text))
            .retrieve()
            .body<ExtractResponse>()
            ?: ExtractResponse()
}
