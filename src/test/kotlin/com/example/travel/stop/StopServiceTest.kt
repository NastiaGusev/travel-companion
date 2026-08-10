package com.example.travel.stop

import com.example.travel.model.entity.Stop
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.StopRepository
import com.example.travel.repository.TripRepository
import com.example.travel.service.StopService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

class StopServiceTest {

    private val service = StopService(
        mock(StopRepository::class.java),
        mock(ItineraryDayRepository::class.java),
        mock(TripRepository::class.java),
    )

    private fun stop(
        name: String,
        startTime: LocalTime?,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
    ) = Stop(
        dayId = UUID.randomUUID(),
        name = name,
        position = 0,
        startTime = startTime,
        endTime = null,
        notes = null,
    ).apply { this.createdAt = createdAt }

    @Test
    fun `orders by start time ascending`() {
        val result = service.orderStops(
            listOf(
                stop("Noon", LocalTime.of(12, 0)),
                stop("Morning", LocalTime.of(8, 0)),
                stop("Evening", LocalTime.of(19, 0)),
            )
        )
        assertThat(result.map { it.name }).containsExactly("Morning", "Noon", "Evening")
    }

    @Test
    fun `stops without a start time sort after timed stops`() {
        val result = service.orderStops(
            listOf(
                stop("Untimed", null),
                stop("Timed", LocalTime.of(9, 0)),
            )
        )
        assertThat(result.map { it.name }).containsExactly("Timed", "Untimed")
    }

    @Test
    fun `equal start times fall back to creation order`() {
        val t0 = OffsetDateTime.now()
        val nine = LocalTime.of(9, 0)
        val result = service.orderStops(
            listOf(
                stop("Second", nine, createdAt = t0.plusSeconds(1)),
                stop("First", nine, createdAt = t0),
            )
        )
        assertThat(result.map { it.name }).containsExactly("First", "Second")
    }

    @Test
    fun `multiple untimed stops keep creation order among themselves`() {
        val t0 = OffsetDateTime.now()
        val result = service.orderStops(
            listOf(
                stop("Untimed B", null, createdAt = t0.plusSeconds(1)),
                stop("Untimed A", null, createdAt = t0),
            )
        )
        assertThat(result.map { it.name }).containsExactly("Untimed A", "Untimed B")
    }

    @Test
    fun `all null times preserve creation order`() {
        val t0 = OffsetDateTime.now()
        val result = service.orderStops(
            listOf(
                stop("Third", null, createdAt = t0.plusSeconds(2)),
                stop("First", null, createdAt = t0),
                stop("Second", null, createdAt = t0.plusSeconds(1)),
            )
        )
        assertThat(result.map { it.name }).containsExactly("First", "Second", "Third")
    }

    @Test
    fun `a single stop is returned as-is`() {
        val result = service.orderStops(listOf(stop("Only", LocalTime.of(10, 0))))
        assertThat(result.map { it.name }).containsExactly("Only")
    }

    @Test
    fun `an empty list returns empty`() {
        assertThat(service.orderStops(emptyList())).isEmpty()
    }
}