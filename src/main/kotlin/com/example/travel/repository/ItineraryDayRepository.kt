package com.example.travel.repository

import com.example.travel.entity.ItineraryDay
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ItineraryDayRepository : JpaRepository<ItineraryDay, UUID> {
    fun findByTripIdOrderByDayNumber(tripId: UUID): List<ItineraryDay>
    fun findTopByTripIdOrderByDayNumberDesc(tripId: UUID): ItineraryDay?
}