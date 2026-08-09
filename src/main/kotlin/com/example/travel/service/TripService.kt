package com.example.travel.service

import com.example.travel.dto.TripRequest
import com.example.travel.dto.TripResponse
import com.example.travel.entity.Trip
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class TripService(
    private val tripRepository: TripRepository,
) {
    fun create(userId: Long, request: TripRequest): TripResponse {
        validateDates(request.startDate, request.endDate)
        val trip = Trip(
            userId = userId,
            title = request.title,
            destination = request.destination,
            startDate = request.startDate,
            endDate = request.endDate,
            description = request.description,
        )
        return tripRepository.save(trip).toResponse()
    }

    fun listForUser(userId: Long): List<TripResponse> =
        tripRepository.findByUserId(userId).map { it.toResponse() }

    fun getForUser(id: Long, userId: Long): TripResponse =
        tripRepository.findByIdAndUserId(id, userId)?.toResponse()
            ?: throw NoSuchElementException("Trip not found")

    fun deleteForUser(id: Long, userId: Long) {
        val trip = tripRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Trip not found")
        tripRepository.delete(trip)
    }

    fun update(userId: Long, id: Long, request: TripRequest): TripResponse {
        val trip = tripRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Trip not found")
        validateDates(request.startDate, request.endDate)
        trip.title = request.title
        trip.destination = request.destination
        trip.startDate = request.startDate
        trip.endDate = request.endDate
        trip.description = request.description
        trip.updatedAt = OffsetDateTime.now()
        return tripRepository.save(trip).toResponse()
    }

    private fun Trip.toResponse() = TripResponse(
        id = id!!,
        title = title,
        destination = destination,
        startDate = startDate,
        endDate = endDate,
        description = description,
    )

    private fun validateDates(startDate: LocalDate?, endDate: LocalDate?) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw IllegalArgumentException("End date must be on or after start date")
        }
    }
}