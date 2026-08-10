package com.example.travel.model.entity

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "itinerary_days")
class ItineraryDay(
    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "day_number", nullable = false)
    var dayNumber: Int,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    var id: UUID? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
}
