package com.example.travel.dto

import java.time.LocalDate
import java.util.UUID

data class DayRequest(
    val notes: String? = null,
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
)