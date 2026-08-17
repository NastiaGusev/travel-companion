package com.example.travel.service

import com.example.travel.exception.AccessDeniedException
import com.example.travel.model.entity.Trip
import com.example.travel.repository.TripCollaboratorRepository
import com.example.travel.repository.TripRepository
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TripAccessService(
    private val tripRepository: TripRepository,
    private val collaboratorRepository: TripCollaboratorRepository,
) {
    /**
     * Returns the trip if the user can edit it (owner or editor).
     * - Non-collaborators get 404 (they can't tell the trip exists).
     */
    fun requireEditAccess(tripId: UUID, userId: UUID): Trip {
        val trip = tripRepository.findById(tripId).orElse(null)
            ?: throw NoSuchElementException("Trip not found")
        val isOwner = trip.userId == userId
        val isEditor = collaboratorRepository.existsByTripIdAndUserId(tripId, userId)
        if (!isOwner && !isEditor) throw NoSuchElementException("Trip not found")
        return trip
    }

    /**
     * Returns the trip only if the user is the owner.
     * - Editors get 403 (they can see it, but this action is owner-only).
     * - Non-collaborators get 404 (they can't tell it exists).
     */
    fun requireOwner(tripId: UUID, userId: UUID): Trip {
        val trip = tripRepository.findById(tripId).orElse(null)
            ?: throw NoSuchElementException("Trip not found")
        if (trip.userId == userId) return trip

        // Not the owner. Distinguish editor (403) from outsider (404).
        val isEditor = collaboratorRepository.existsByTripIdAndUserId(tripId, userId)
        if (isEditor) throw AccessDeniedException("Only the trip owner can perform this action")
        throw NoSuchElementException("Trip not found")
    }

    /** All trips the user can access: owned plus shared-with-them, deduplicated. */
    fun listAccessibleTrips(userId: UUID): List<Trip> {
        val owned = tripRepository.findByUserId(userId)
        val sharedTripIds = collaboratorRepository.findByUserId(userId).map { it.tripId }
        val shared = if (sharedTripIds.isEmpty()) emptyList()
        else tripRepository.findAllById(sharedTripIds)
        return (owned + shared).distinctBy { it.id }
    }

    fun requireMatchingVersion(current: Long, expected: Long?) {
        if (expected == null) {
            throw IllegalArgumentException("version is required for updates")
        }
        if (current != expected) {
            throw ObjectOptimisticLockingFailureException(
                "Item was modified by someone else", null,
            )
        }
    }
}