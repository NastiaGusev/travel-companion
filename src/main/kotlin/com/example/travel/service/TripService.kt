package com.example.travel.service

import com.example.travel.model.dto.TripRequest
import com.example.travel.model.dto.TripResponse
import com.example.travel.model.entity.Trip
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val dayRepository: ItineraryDayRepository,
    private val tripAccessService: TripAccessService,
    ) {
    fun create(userId: UUID, request: TripRequest): TripResponse {
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

    fun listForUser(userId: UUID): List<TripResponse> =
        tripAccessService.listAccessibleTrips(userId).map { it.toResponse() }

    fun getForUser(id: UUID, userId: UUID): TripResponse =
        tripAccessService.requireEditAccess(id, userId).toResponse()

    fun deleteForUser(id: UUID, userId: UUID) {
        val trip = tripAccessService.requireOwner(id, userId)
        tripRepository.delete(trip)
    }

    fun update(userId: UUID, id: UUID, request: TripRequest): TripResponse {
        val trip = tripAccessService.requireEditAccess(id, userId)
        validateDates(request.startDate, request.endDate)

        tripAccessService.requireMatchingVersion(trip.version, request.version)

        // If dates are being set/shrunk, ensure the existing day count still fits
        if (request.startDate != null && request.endDate != null) {
            val newLength = ChronoUnit.DAYS.between(request.startDate, request.endDate).toInt() + 1
            val existingDayCount = dayRepository.findByTripIdOrderByDayNumber(id).size
            if (existingDayCount > newLength) {
                throw IllegalArgumentException(
                    "Cannot shorten this trip to $newLength day(s): it has $existingDayCount day(s). " +
                            "Delete the extra days first, then adjust the trip."
                )
            }
        }

        trip.title = request.title
        trip.destination = request.destination
        trip.startDate = request.startDate
        trip.endDate = request.endDate
        trip.description = request.description
        trip.updatedAt = OffsetDateTime.now()
        return tripRepository.saveAndFlush(trip).toResponse()
    }

    private fun Trip.toResponse() = TripResponse(
        id = id!!,
        title = title,
        destination = destination,
        startDate = startDate,
        endDate = endDate,
        description = description,
        version = version,
    )

    private fun validateDates(startDate: LocalDate?, endDate: LocalDate?) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw IllegalArgumentException("End date must be on or after start date")
        }
    }
}