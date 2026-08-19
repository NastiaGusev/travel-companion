package com.example.travel.client.googlePlaces

import com.example.travel.client.googlePlaces.dto.AutocompleteRequest
import com.example.travel.client.googlePlaces.dto.AutocompleteResponse
import com.example.travel.client.googlePlaces.dto.Circle
import com.example.travel.client.googlePlaces.dto.LatLng
import com.example.travel.client.googlePlaces.dto.LocationBias
import com.example.travel.client.googlePlaces.dto.PlaceDetailsResponse
import com.example.travel.client.googlePlaces.dto.PlacePrediction
import com.example.travel.client.googlePlaces.dto.ResolvedPlaceData
import com.example.travel.exception.PlacesUnavailableException
import org.slf4j.LoggerFactory
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class PlacesClient(
    private val placesRestClient: RestClient,
    @Value($$"${google.places.api-key}") private val apiKey: String,
) {

    /** Autocomplete, biased toward the trip's destination if it has one. */
    @CircuitBreaker(name = "googlePlaces", fallbackMethod = "autocompleteFallback")
    @Retry(name = "googlePlaces")
    fun autocomplete(query: String, biasLat: Double?, biasLng: Double?): List<PlacePrediction> {
        val body = AutocompleteRequest(
            input = query,
            locationBias = buildBias(biasLat, biasLng),
        )

        val response = placesRestClient.post()
            .uri("/places:autocomplete")
            .header("X-Goog-Api-Key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body<AutocompleteResponse>()

        return response?.suggestions.orEmpty().mapNotNull { it.placePrediction }
            .map {
                PlacePrediction(
                    placeId = it.placeId,
                    description = it.text?.text ?: "",
                )
            }
    }

    /** Full details for a chosen place. Field mask keeps this in the cheap tier. */
    @CircuitBreaker(name = "googlePlaces")
    @Retry(name = "googlePlaces")
    fun details(placeId: String): ResolvedPlaceData {
        val response = try {
            placesRestClient.get()
                .uri("/places/{placeId}", placeId)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "id,displayName,location,formattedAddress,types")
                .retrieve()
                .body<PlaceDetailsResponse>()
        } catch (ex: NoSuchElementException) {
            throw ex
        } catch (ex: Exception) {
            throw PlacesUnavailableException("Place lookup failed", ex)
        } ?: throw NoSuchElementException("Place not found: $placeId")

        return ResolvedPlaceData(
            placeId = response.id,
            name = response.displayName?.text,
            latitude = response.location?.latitude,
            longitude = response.location?.longitude,
            address = response.formattedAddress,
            category = response.types?.firstOrNull(),
        )
    }

    private fun buildBias(lat: Double?, lng: Double?): LocationBias? {
        if (lat == null || lng == null) return null
        return LocationBias(
            circle = Circle(
                center = LatLng(lat, lng),
                radius = 50_000.0,
            ),
        )
    }

    @Suppress("unused")
    private fun autocompleteFallback(
        query: String,
        biasLat: Double?,
        biasLng: Double?,
        ex: Exception,
    ): List<PlacePrediction> {
        LoggerFactory.getLogger(PlacesClient::class.java).warn("Places autocomplete unavailable, returning empty results: ${ex.message}")
        return emptyList()
    }
}