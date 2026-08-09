package com.example.travel.repository

import com.example.travel.entity.ItineraryDay
import org.springframework.data.jpa.repository.JpaRepository

interface ItineraryDayRepository : JpaRepository<ItineraryDay, Long> {
    fun findByTripId(tripId: Long): List<ItineraryDay>
    fun findTopByTripIdOrderByDayNumberDesc(tripId: Long): ItineraryDay?
}