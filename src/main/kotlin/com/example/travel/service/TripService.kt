package com.example.travel.service

import com.example.travel.client.googlePlaces.PlacesClient
import com.example.travel.model.dto.DestinationDto
import com.example.travel.model.dto.TripRequest
import com.example.travel.model.dto.TripResponse
import com.example.travel.model.entity.Destination
import com.example.travel.model.entity.Trip
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val dayRepository: ItineraryDayRepository,
    private val tripAccessService: TripAccessService,
    private val placesClient: PlacesClient
) {
    fun create(userId: UUID, request: TripRequest): TripResponse {
        validateDates(request.startDate, request.endDate)

        val trip = Trip(
            userId = userId,
            title = request.title,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            destination = resolveDestination(request),
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
        trip.startDate = request.startDate
        trip.endDate = request.endDate
        trip.description = request.description
        trip.updatedAt = OffsetDateTime.now()
        trip.destination = resolveDestination(request)

        return tripRepository.saveAndFlush(trip).toResponse()
    }

    private fun resolveDestination(request: TripRequest): Destination? {
        val anchor = request.destinationPlaceId?.let { placesClient.details(it) }
        return when {
            anchor != null -> Destination(
                placeName = request.destinationName ?: anchor.name,
                placeId = anchor.placeId,
                latitude = anchor.latitude,
                longitude = anchor.longitude,
            )

            request.destinationName != null -> Destination(
                placeName = request.destinationName,
                placeId = null,
                latitude = null,
                longitude = null,
            )

            else -> null
        }
    }

    private fun Trip.toResponse() = TripResponse(
        id = id!!,
        title = title,
        destination = destination?.let {
            DestinationDto(
                name = it.placeName,
                placeId = it.placeId,
                latitude = it.latitude,
                longitude = it.longitude,
            )
        },
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