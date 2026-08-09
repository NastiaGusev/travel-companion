package com.example.travel.service

import com.example.travel.dto.DayRequest
import com.example.travel.dto.DayResponse
import com.example.travel.entity.ItineraryDay
import com.example.travel.entity.Trip
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ItineraryDayService(
    private val dayRepository: ItineraryDayRepository,
    private val tripRepository: TripRepository,
) {
    fun create(userId: UUID, tripId: UUID, request: DayRequest): DayResponse {
        val trip = tripRepository.findByIdAndUserId(tripId, userId)
            ?: throw NoSuchElementException("Trip not found")

        val nextDayNumber = (dayRepository
            .findTopByTripIdOrderByDayNumberDesc(tripId)?.dayNumber ?: 0) + 1

        validateDayFitsTrip(trip, nextDayNumber)

        val day = ItineraryDay(
            tripId = tripId,
            dayNumber = nextDayNumber,
            notes = request.notes,
        )
        return dayRepository.save(day).toResponse(trip)
    }

    @Transactional
    fun delete(userId: UUID, dayId: UUID): List<DayResponse> {
        val day = dayRepository.findById(dayId)
            .orElseThrow { NoSuchElementException("Day not found") }
        val trip = tripRepository.findByIdAndUserId(day.tripId, userId)
            ?: throw NoSuchElementException("Trip not found")

        val deletedNumber = day.dayNumber
        dayRepository.delete(day)

        val toShift = dayRepository.findByTripId(trip.id!!)
            .filter { it.dayNumber > deletedNumber }
        toShift.forEach { it.dayNumber -= 1 }
        dayRepository.saveAll(toShift)

        return dayRepository.findByTripId(trip.id!!)
            .sortedBy { it.dayNumber }
            .map { it.toResponse(trip) }
    }

    @Transactional
    fun updateNotes(userId: UUID, dayId: UUID, notes: String?): DayResponse {
        val day = dayRepository.findById(dayId)
            .orElseThrow { NoSuchElementException("Day not found") }
        val trip = tripRepository.findByIdAndUserId(day.tripId, userId)
            ?: throw NoSuchElementException("Trip not found")
        day.notes = notes
        day.updatedAt = OffsetDateTime.now()
        return dayRepository.save(day).toResponse(trip)
    }

    fun listForTrip(userId: UUID, tripId: UUID): List<DayResponse> {
        val trip = tripRepository.findByIdAndUserId(tripId, userId)
            ?: throw NoSuchElementException("Trip not found")
        return dayRepository.findByTripId(tripId).map { it.toResponse(trip) }
    }

    @Transactional
    fun swapDays(userId: UUID, tripId: UUID, dayIdA: UUID, dayIdB: UUID): List<DayResponse> {
        val trip = tripRepository.findByIdAndUserId(tripId, userId)
            ?: throw NoSuchElementException("Trip not found")

        val dayA = dayRepository.findById(dayIdA)
            .orElseThrow { NoSuchElementException("Day $dayIdA not found") }
        val dayB = dayRepository.findById(dayIdB)
            .orElseThrow { NoSuchElementException("Day $dayIdB not found") }

        // Both days must belong to this trip (and thus this user)
        require(dayA.tripId == tripId && dayB.tripId == tripId) {
            "Both days must belong to the specified trip"
        }

        val numberA = dayA.dayNumber
        val numberB = dayB.dayNumber

        // Park A at a temporary number to avoid the unique-constraint collision
        dayA.dayNumber = -1
        dayRepository.saveAndFlush(dayA)

        dayB.dayNumber = numberA
        dayRepository.saveAndFlush(dayB)

        dayA.dayNumber = numberB
        dayRepository.saveAndFlush(dayA)

        return dayRepository.findByTripId(tripId)
            .sortedBy { it.dayNumber }
            .map { it.toResponse(trip) }
    }

    private fun ItineraryDay.toResponse(trip: Trip) = DayResponse(
        id = id!!,
        tripId = tripId,
        dayNumber = dayNumber,
        dayDate = trip.startDate?.plusDays((dayNumber - 1).toLong()),
        notes = notes,
    )

    private fun validateDayFitsTrip(trip: Trip, dayNumber: Int) {
        val start = trip.startDate
        val end = trip.endDate
        if (start != null && end != null) {
            val tripLength = ChronoUnit.DAYS.between(start, end).toInt() + 1
            if (dayNumber > tripLength) {
                throw IllegalArgumentException(
                    "This trip is $tripLength day(s) long and already has that many days. " +
                            "Extend the trip's dates to add more."
                )
            }
        }
    }
}