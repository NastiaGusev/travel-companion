package com.example.travel.service

import com.example.travel.model.dto.CollaboratorResponse
import com.example.travel.model.entity.TripCollaborator
import com.example.travel.repository.TripCollaboratorRepository
import com.example.travel.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CollaboratorService(
    private val collaboratorRepository: TripCollaboratorRepository,
    private val userRepository: UserRepository,
    private val tripAccessService: TripAccessService,
) {
    fun addEditor(tripId: UUID, ownerId: UUID, email: String): CollaboratorResponse {
        tripAccessService.requireOwner(tripId, ownerId)   // only owner can add

        val user = userRepository.findByEmail(email)
            ?: throw NoSuchElementException("User not found")

        // Can't add the owner as a collaborator of their own trip
        if (user.id == ownerId) {
            throw IllegalArgumentException("The owner is already on this trip")
        }
        // Already a collaborator?
        if (collaboratorRepository.existsByTripIdAndUserId(tripId, user.id!!)) {
            throw IllegalArgumentException("User is already a collaborator")
        }

        val saved = collaboratorRepository.save(
            TripCollaborator(tripId = tripId, userId = user.id!!, role = "EDITOR"),
        )
        return CollaboratorResponse(userId = saved.userId, email = user.email, role = saved.role)
    }

    fun listCollaborators(tripId: UUID, userId: UUID): List<CollaboratorResponse> {
        val trip = tripAccessService.requireEditAccess(tripId, userId)

        val owner = userRepository.findById(trip.userId).orElse(null)
        val ownerEntry = owner?.let {
            CollaboratorResponse(userId = it.id!!, email = it.email, role = "OWNER")
        }

        val editors = collaboratorRepository.findByTripId(tripId).map { c ->
            val u = userRepository.findById(c.userId).orElse(null)
            CollaboratorResponse(userId = c.userId, email = u?.email ?: "(unknown)", role = c.role)
        }

        return listOfNotNull(ownerEntry) + editors
    }

    @Transactional
    fun removeEditor(tripId: UUID, ownerId: UUID, targetUserId: UUID) {
        tripAccessService.requireOwner(tripId, ownerId)   // only owner can remove
        if (!collaboratorRepository.existsByTripIdAndUserId(tripId, targetUserId)) {
            throw NoSuchElementException("Collaborator not found")
        }
        collaboratorRepository.deleteByTripIdAndUserId(tripId, targetUserId)
    }
}