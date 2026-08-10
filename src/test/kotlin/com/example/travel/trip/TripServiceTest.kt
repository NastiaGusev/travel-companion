package com.example.travel.trip

import com.example.travel.dto.TripRequest
import com.example.travel.entity.ItineraryDay
import com.example.travel.entity.Trip
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.TripRepository
import com.example.travel.service.TripService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TripServiceTest {

    @Mock
    lateinit var tripRepository: TripRepository

    @Mock
    lateinit var dayRepository: ItineraryDayRepository

    private val service by lazy { TripService(tripRepository, dayRepository) }

    @Test
    fun `update rejects shrinking a trip below its existing day count`() {
        val userId = UUID.randomUUID()
        val tripId = UUID.randomUUID()

        // existing trip: a 3-day trip (May 1–3)
        val existing = Trip(
            userId = userId,
            title = "Japan",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 3),
        ).apply { id = tripId }

        whenever(tripRepository.findByIdAndUserId(tripId, userId)).thenReturn(existing)
        // three days already exist on this trip
        whenever(dayRepository.findByTripIdOrderByDayNumber(tripId))
            .thenReturn(listOf(itineraryDay(tripId, 1), itineraryDay(tripId, 2), itineraryDay(tripId, 3)))

        // attempt to shrink to a 2-day trip (May 1–2)
        val request = TripRequest(
            title = "Japan",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 2),
        )

        assertThatThrownBy { service.update(userId, tripId, request) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `update allows shrinking when the new length still fits the existing days`() {
        val userId = UUID.randomUUID()
        val tripId = UUID.randomUUID()

        val existing = Trip(
            userId = userId,
            title = "Japan",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 5),
        ).apply { id = tripId }

        whenever(tripRepository.findByIdAndUserId(tripId, userId)).thenReturn(existing)
        whenever(dayRepository.findByTripIdOrderByDayNumber(tripId))
            .thenReturn(listOf(itineraryDay(tripId, 1), itineraryDay(tripId, 2)))
        whenever(tripRepository.save(any<Trip>())).thenAnswer { it.arguments[0] as Trip }

        // shrink 5-day trip to 3 days — still fits the 2 existing days
        val request = TripRequest(
            title = "Japan",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 3),
        )

        val result = service.update(userId, tripId, request)

        assertThat(result.endDate).isEqualTo(LocalDate.of(2026, 5, 3))
    }

    private fun itineraryDay(tripId: UUID, number: Int) =
        ItineraryDay(tripId = tripId, dayNumber = number)
}