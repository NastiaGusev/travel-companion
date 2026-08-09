package com.example.travel.service

import com.example.travel.dto.DayRequest
import com.example.travel.dto.DayResponse
import com.example.travel.entity.ItineraryDay
import com.example.travel.entity.Trip
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit

@Service
class ItineraryDayService(
    private val dayRepository: ItineraryDayRepository,
    private val tripRepository: TripRepository,
) {
    fun create(userId: Long, tripId: Long, request: DayRequest): DayResponse {
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

    fun listForTrip(userId: Long, tripId: Long): List<DayResponse> {
        val trip = tripRepository.findByIdAndUserId(tripId, userId)
            ?: throw NoSuchElementException("Trip not found")
        return dayRepository.findByTripId(tripId).map { it.toResponse(trip) }
    }

    @Transactional
    fun delete(userId: Long, dayId: Long): List<DayResponse> {
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