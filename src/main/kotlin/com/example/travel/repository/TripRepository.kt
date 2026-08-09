package com.example.travel.repository

import com.example.travel.entity.Trip
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripRepository : JpaRepository<Trip, UUID> {
    fun findByUserId(userId: UUID): List<Trip>
    fun findByIdAndUserId(id: UUID, userId: UUID): Trip?
}