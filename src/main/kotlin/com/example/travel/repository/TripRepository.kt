package com.example.travel.repository

import com.example.travel.entity.Trip
import org.springframework.data.jpa.repository.JpaRepository

interface TripRepository : JpaRepository<Trip, Long> {
    fun findByUserId(userId: Long): List<Trip>
    fun findByIdAndUserId(id: Long, userId: Long): Trip?
}