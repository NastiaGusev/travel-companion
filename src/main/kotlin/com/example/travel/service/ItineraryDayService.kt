package com.example.travel.service

import com.example.travel.dto.DayRequest
import com.example.travel.dto.DayResponse
import com.example.travel.entity.ItineraryDay
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service

@Service
class ItineraryDayService(
    private val dayRepository: ItineraryDayRepository,
    private val tripRepository: TripRepository,
) {
    fun create(userId: Long, tripId: Long, request: DayRequest): DayResponse {
        verifyTripOwnership(tripId, userId)
        val day = ItineraryDay(
            tripId = tripId,
            dayNumber = request.dayNumber,
            dayDate = request.dayDate,
            notes = request.notes,
        )
        return dayRepository.save(day).toResponse()
    }

    fun listForTrip(userId: Long, tripId: Long): List<DayResponse> {
        verifyTripOwnership(tripId, userId)
        return dayRepository.findByTripId(tripId).map { it.toResponse() }
    }

    fun delete(userId: Long, dayId: Long) {
        val day = dayRepository.findById(dayId)
            .orElseThrow { NoSuchElementException("Day not found") }
        verifyTripOwnership(day.tripId, userId)   // ownership flows up: day → trip → user
        dayRepository.delete(day)
    }

    private fun verifyTripOwnership(tripId: Long, userId: Long) {
        tripRepository.findByIdAndUserId(tripId, userId)
            ?: throw NoSuchElementException("Trip not found")
    }

    private fun ItineraryDay.toResponse() = DayResponse(
        id = id!!, tripId = tripId, dayNumber = dayNumber, dayDate = dayDate, notes = notes,
    )
}