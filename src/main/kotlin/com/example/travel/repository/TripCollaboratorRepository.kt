package com.example.travel.repository

import com.example.travel.model.entity.TripCollaborator
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripCollaboratorRepository : JpaRepository<TripCollaborator, UUID> {
    fun findByTripId(tripId: UUID): List<TripCollaborator>
    fun existsByTripIdAndUserId(tripId: UUID, userId: UUID): Boolean
    fun deleteByTripIdAndUserId(tripId: UUID, userId: UUID)
    fun findByUserId(userId: UUID): List<TripCollaborator>
}