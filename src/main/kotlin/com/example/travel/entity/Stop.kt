package com.example.travel.entity

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "stops")
class Stop(
    @Column(name = "itinerary_day_id", nullable = false)
    var dayId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var position: Int,

    @Column(name = "start_time")
    var startTime: LocalTime? = null,

    @Column(name = "end_time")
    var endTime: LocalTime? = null,

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