package com.example.travel.repository

import com.example.travel.model.entity.Stop
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StopRepository : JpaRepository<Stop, UUID> {
    fun findByDayIdOrderByPosition(dayId: UUID): List<Stop>
}