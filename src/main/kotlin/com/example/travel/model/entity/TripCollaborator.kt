package com.example.travel.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "trip_collaborators")
class TripCollaborator(
    @Id @GeneratedValue
    var id: UUID? = null,
    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,
    @Column(name = "user_id", nullable = false)
    var userId: UUID,
    @Column(nullable = false)
    var role: String,   // "EDITOR"
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)