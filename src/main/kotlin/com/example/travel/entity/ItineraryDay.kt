package com.example.travel.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "itinerary_days")
class ItineraryDay(
    @Column(name = "trip_id", nullable = false)
    var tripId: Long,

    @Column(name = "day_number", nullable = false)
    var dayNumber: Int,

    @Column(name = "day_date")
    var dayDate: LocalDate? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
}