package com.example.travel.model.entity

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "trips")
class Trip(
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Embedded
    var destination: Destination? = null,
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

    @Version
    @Column(nullable = false)
    var version: Long = 0
}

@Embeddable
class Destination(
    @Column(name = "place_name")
    var placeName: String? = null,
    @Column(name = "google_place_id")
    var placeId: String? = null,
    @Column(name = "place_latitude")
    var latitude: Double? = null,
    @Column(name = "place_longitude")
    var longitude: Double? = null,
)
