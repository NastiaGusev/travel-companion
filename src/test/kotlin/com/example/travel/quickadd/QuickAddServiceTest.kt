package com.example.travel.quickadd

import com.example.travel.client.aiService.AiExtractionClient
import com.example.travel.client.aiService.dto.ExtractResponse
import com.example.travel.client.aiService.dto.ExtractedDay
import com.example.travel.client.aiService.dto.ExtractedStop
import com.example.travel.client.googlePlaces.PlacesClient
import com.example.travel.client.googlePlaces.dto.PlacePrediction
import com.example.travel.client.googlePlaces.dto.ResolvedPlaceData
import com.example.travel.exception.AiServiceUnavailableException
import com.example.travel.exception.PlaceNotFoundException
import com.example.travel.model.dto.DayRequest
import com.example.travel.model.dto.DayResponse
import com.example.travel.model.dto.QuickAddRequest
import com.example.travel.model.dto.StopRequest
import com.example.travel.model.dto.StopResponse
import com.example.travel.model.entity.Destination
import com.example.travel.model.entity.Trip
import com.example.travel.service.ItineraryDayService
import com.example.travel.service.QuickAddService
import com.example.travel.service.StopService
import com.example.travel.service.TripAccessService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class QuickAddServiceTest {

    @Mock lateinit var aiExtractionClient: AiExtractionClient
    @Mock lateinit var tripAccessService: TripAccessService
    @Mock lateinit var placesClient: PlacesClient
    @Mock lateinit var dayService: ItineraryDayService
    @Mock lateinit var stopService: StopService

    private val service by lazy {
        QuickAddService(aiExtractionClient, tripAccessService, placesClient, dayService, stopService)
    }

    private val userId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()

    private fun trip(destination: Destination? = null) =
        Trip(userId = userId, title = "Japan", destination = destination).apply { id = tripId }

    private fun dayResponse(number: Int) = DayResponse(
        id = UUID.randomUUID(), tripId = tripId, dayNumber = number, dayDate = null, notes = null, version = 0,
    )

    private fun stopResponse(title: String) = StopResponse(
        id = UUID.randomUUID(), dayId = UUID.randomUUID(), title = title, position = 1,
        startTime = null, endTime = null, notes = null, place = null,
    )

    @Test
    fun `creates one day and stop per extracted item, resolving each stop through Places`() {
        val destination = Destination(latitude = 35.0, longitude = 139.0)
        whenever(tripAccessService.requireEditAccess(tripId, userId)).thenReturn(trip(destination))
        whenever(aiExtractionClient.extract("some text")).thenReturn(
            ExtractResponse(
                days = listOf(
                    ExtractedDay(
                        position = 1,
                        stops = listOf(ExtractedStop(searchQuery = "Tsukiji Market", name = "Tsukiji Market", timeHint = "12:30")),
                    ),
                ),
            )
        )
        val day = dayResponse(1)
        whenever(dayService.create(userId, tripId, DayRequest())).thenReturn(day)
        whenever(placesClient.autocomplete("Tsukiji Market", 35.0, 139.0))
            .thenReturn(listOf(PlacePrediction(placeId = "ChIJxyz", description = "Tsukiji Market, Tokyo")))
        whenever(placesClient.details("ChIJxyz")).thenReturn(
            ResolvedPlaceData("ChIJxyz", "Tsukiji Market", 35.0, 139.0, "Tokyo", "market")
        )
        val stop = stopResponse("Tsukiji Market")
        whenever(stopService.create(eq(userId), eq(day.id), any())).thenReturn(stop)

        val result = service.quickAdd(userId, tripId, QuickAddRequest("some text"))

        assertThat(result.days).hasSize(1)
        assertThat(result.days[0].dayId).isEqualTo(day.id)
        assertThat(result.days[0].stops).containsExactly(stop)

        verify(stopService).create(
            userId, day.id,
            StopRequest(title = "Tsukiji Market", startTime = LocalTime.of(12, 30), placeId = "ChIJxyz"),
        )
    }

    @Test
    fun `nothing extractable returns an empty day list without creating anything`() {
        whenever(tripAccessService.requireEditAccess(tripId, userId)).thenReturn(trip())
        whenever(aiExtractionClient.extract("asdf")).thenReturn(ExtractResponse(days = emptyList()))

        val result = service.quickAdd(userId, tripId, QuickAddRequest("asdf"))

        assertThat(result.days).isEmpty()
        verifyNoInteractions(dayService, stopService, placesClient)
    }

    @Test
    fun `a place that fails to resolve falls back to a title-only stop instead of failing the request`() {
        whenever(tripAccessService.requireEditAccess(tripId, userId)).thenReturn(trip())
        whenever(aiExtractionClient.extract(any())).thenReturn(
            ExtractResponse(
                days = listOf(
                    ExtractedDay(position = 1, stops = listOf(ExtractedStop(searchQuery = "Nowhere", name = "Nowhere"))),
                ),
            )
        )
        val day = dayResponse(1)
        whenever(dayService.create(userId, tripId, DayRequest())).thenReturn(day)
        whenever(placesClient.autocomplete("Nowhere", null, null))
            .thenReturn(listOf(PlacePrediction(placeId = "ChIJstale", description = "Nowhere")))
        whenever(placesClient.details("ChIJstale")).thenThrow(PlaceNotFoundException("ChIJstale"))
        whenever(stopService.create(any(), any(), any())).thenReturn(stopResponse("Nowhere"))

        service.quickAdd(userId, tripId, QuickAddRequest("text"))

        verify(stopService).create(
            userId, day.id,
            StopRequest(title = "Nowhere", startTime = null, placeId = null),
        )
    }

    @Test
    fun `an ai-service outage is wrapped into AiServiceUnavailableException`() {
        whenever(tripAccessService.requireEditAccess(tripId, userId)).thenReturn(trip())
        whenever(aiExtractionClient.extract(any())).thenThrow(
            HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "boom", HttpHeaders.EMPTY, ByteArray(0), null)
        )

        assertThatThrownBy { service.quickAdd(userId, tripId, QuickAddRequest("text")) }
            .isInstanceOf(AiServiceUnavailableException::class.java)

        verifyNoInteractions(dayService, stopService)
    }

    @Test
    fun `maps morning afternoon evening hints to default times`() {
        whenever(tripAccessService.requireEditAccess(tripId, userId)).thenReturn(trip())
        whenever(aiExtractionClient.extract(any())).thenReturn(
            ExtractResponse(
                days = listOf(
                    ExtractedDay(
                        position = 1,
                        stops = listOf(ExtractedStop(searchQuery = "TeamLab", name = "TeamLab", timeHint = "afternoon")),
                    ),
                ),
            )
        )
        val day = dayResponse(1)
        whenever(dayService.create(userId, tripId, DayRequest())).thenReturn(day)
        whenever(placesClient.autocomplete("TeamLab", null, null)).thenReturn(emptyList())
        whenever(stopService.create(any(), any(), any())).thenReturn(stopResponse("TeamLab"))

        service.quickAdd(userId, tripId, QuickAddRequest("text"))

        verify(stopService).create(
            userId, day.id,
            StopRequest(title = "TeamLab", startTime = LocalTime.of(13, 0), placeId = null),
        )
    }
}
