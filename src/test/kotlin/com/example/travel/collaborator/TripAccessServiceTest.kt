package com.example.travel.collaborator

import com.example.travel.model.entity.Trip
import com.example.travel.repository.TripCollaboratorRepository
import com.example.travel.repository.TripRepository
import com.example.travel.exception.AccessDeniedException
import com.example.travel.service.TripAccessService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TripAccessServiceTest {

    @Mock lateinit var tripRepository: TripRepository
    @Mock lateinit var collaboratorRepository: TripCollaboratorRepository

    private val service by lazy { TripAccessService(tripRepository, collaboratorRepository) }

    private fun trip(ownerId: UUID, tripId: UUID) =
        Trip(userId = ownerId, title = "T").apply { id = tripId }

    // ---- requireEditAccess ----

    @Test
    fun `requireEditAccess returns the trip for the owner`() {
        val ownerId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val t = trip(ownerId, tripId)
        whenever(tripRepository.findById(tripId)).thenReturn(Optional.of(t))

        val result = service.requireEditAccess(tripId, ownerId)

        assertThat(result).isSameAs(t)
    }

    @Test
    fun `requireEditAccess returns the trip for an editor`() {
        val ownerId = UUID.randomUUID()
        val editorId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val t = trip(ownerId, tripId)
        whenever(tripRepository.findById(tripId)).thenReturn(Optional.of(t))
        whenever(collaboratorRepository.existsByTripIdAndUserId(tripId, editorId)).thenReturn(true)

        val result = service.requireEditAccess(tripId, editorId)

        assertThat(result).isSameAs(t)
    }

    @Test
    fun `requireEditAccess throws NotFound for a non-collaborator`() {
        val ownerId = UUID.randomUUID()
        val outsiderId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val t = trip(ownerId, tripId)
        whenever(tripRepository.findById(tripId)).thenReturn(Optional.of(t))
        whenever(collaboratorRepository.existsByTripIdAndUserId(tripId, outsiderId)).thenReturn(false)

        assertThatThrownBy { service.requireEditAccess(tripId, outsiderId) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    // ---- requireOwner ----

    @Test
    fun `requireOwner returns the trip for the owner`() {
        val ownerId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val t = trip(ownerId, tripId)
        whenever(tripRepository.findById(tripId)).thenReturn(Optional.of(t))

        val result = service.requireOwner(tripId, ownerId)

        assertThat(result).isSameAs(t)
    }

    @Test
    fun `requireOwner throws AccessDenied for an editor`() {
        val ownerId = UUID.randomUUID()
        val editorId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val t = trip(ownerId, tripId)
        whenever(tripRepository.findById(tripId)).thenReturn(Optional.of(t))
        whenever(collaboratorRepository.existsByTripIdAndUserId(tripId, editorId)).thenReturn(true)

        assertThatThrownBy { service.requireOwner(tripId, editorId) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `requireOwner throws NotFound for a non-collaborator`() {
        val ownerId = UUID.randomUUID()
        val outsiderId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val t = trip(ownerId, tripId)
        whenever(tripRepository.findById(tripId)).thenReturn(Optional.of(t))
        whenever(collaboratorRepository.existsByTripIdAndUserId(tripId, outsiderId)).thenReturn(false)

        assertThatThrownBy { service.requireOwner(tripId, outsiderId) }
            .isInstanceOf(NoSuchElementException::class.java)
    }
}