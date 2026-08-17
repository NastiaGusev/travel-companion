package com.example.travel.model.dto

import java.time.LocalDate
import java.util.UUID

data class DayRequest(
    val notes: String? = null,
    val version: Long? = null,
)

data class SwapDaysRequest(
    val dayIdA: UUID,
    val dayIdB: UUID,
)

data class DayResponse(
    val id: UUID,
    val tripId: UUID,
    val dayNumber: Int,
    val dayDate: LocalDate?,
    val notes: String?,
    val version: Long,
)