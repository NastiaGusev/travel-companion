package com.example.travel.service

import com.example.travel.client.aiService.AiExtractionClient
import com.example.travel.client.aiService.dto.ExtractedStop
import com.example.travel.client.googlePlaces.PlacesClient
import com.example.travel.exception.AiServiceUnavailableException
import com.example.travel.exception.PlaceNotFoundException
import com.example.travel.model.dto.DayRequest
import com.example.travel.model.dto.QuickAddDayResult
import com.example.travel.model.dto.QuickAddRequest
import com.example.travel.model.dto.QuickAddResponse
import com.example.travel.model.dto.StopRequest
import com.example.travel.model.entity.Trip
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Orchestrates the quick-add flow: free text -> ai-service extraction -> Places resolution ->
 * days/stops saved on the trip.
 *
 * Deliberately reuses ItineraryDayService.create and StopService.create rather than writing to
 * the repositories directly, so a quick-added day/stop goes through exactly the same validation,
 * day-numbering, and resequencing as one entered by hand — there is only one write path, the AI
 * flow just calls it repeatedly.
 */
@Service
class QuickAddService(
    private val aiExtractionClient: AiExtractionClient,
    private val tripAccessService: TripAccessService,
    private val placesClient: PlacesClient,
    private val dayService: ItineraryDayService,
    private val stopService: StopService,
) {
    @Transactional
    fun quickAdd(userId: UUID, tripId: UUID, request: QuickAddRequest): QuickAddResponse {
        val trip = tripAccessService.requireEditAccess(tripId, userId)
        val extraction = extract(request.text)

        val days = extraction.days.map { extractedDay ->
            val day = dayService.create(userId, tripId, DayRequest())
            val stops = extractedDay.stops.map { extractedStop ->
                stopService.create(userId, day.id, toStopRequest(extractedStop, trip))
            }
            QuickAddDayResult(dayId = day.id, dayNumber = day.dayNumber, stops = stops)
        }

        return QuickAddResponse(days = days)
    }

    /**
     * Raw HttpServerErrorException / ResourceAccessException / CallNotPermittedException are
     * caught HERE (outside the @Retry/@CircuitBreaker-annotated extract() method) so retry and
     * the breaker still observe them, and rethrown as AiServiceUnavailableException so
     * GlobalExceptionHandler can't confuse an ai-service outage with a Places outage even though
     * both clients throw the same raw types.
     */
    private fun extract(text: String) =
        try {
            aiExtractionClient.extract(text)
        } catch (e: HttpServerErrorException) {
            throw AiServiceUnavailableException("AI extraction service is unavailable", e)
        } catch (e: ResourceAccessException) {
            throw AiServiceUnavailableException("AI extraction service is unavailable", e)
        } catch (e: CallNotPermittedException) {
            throw AiServiceUnavailableException("AI extraction service is unavailable", e)
        }

    private fun toStopRequest(stop: ExtractedStop, trip: Trip) = StopRequest(
        title = stop.name,
        startTime = resolveStartTime(stop.timeHint),
        placeId = resolvePlaceId(stop.searchQuery, trip),
    )

    /**
     * Searches Places for the extracted query and validates the top hit resolves before handing
     * it to StopService. Any Places-side failure (no match, breaker open, transient 5xx) degrades
     * to null rather than failing the whole quick-add — the stop is still created from its
     * extracted name, just without a resolved location. A bad/flaky Places lookup for one stop
     * should never roll back an otherwise-good multi-day quick-add.
     */
    private fun resolvePlaceId(searchQuery: String, trip: Trip): String? {
        val candidateId = placesClient
            .autocomplete(searchQuery, trip.destination?.latitude, trip.destination?.longitude)
            .firstOrNull()
            ?.placeId
            ?: return null

        return try {
            placesClient.details(candidateId) // details() is Caffeine-cached, so this is not a wasted call
            candidateId
        } catch (e: PlaceNotFoundException) {
            null
        } catch (e: HttpServerErrorException) {
            null
        } catch (e: ResourceAccessException) {
            null
        } catch (e: CallNotPermittedException) {
            null
        }
    }

    private fun resolveStartTime(timeHint: String?): LocalTime? {
        if (timeHint == null) return null
        return when (timeHint.lowercase()) {
            "morning" -> LocalTime.of(9, 0)
            "afternoon" -> LocalTime.of(13, 0)
            "evening" -> LocalTime.of(18, 0)
            else -> try {
                LocalTime.parse(timeHint)
            } catch (e: DateTimeParseException) {
                null // an unparseable hint from the extractor shouldn't fail stop creation
            }
        }
    }
}
