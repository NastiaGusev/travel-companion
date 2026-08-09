package com.example.travel.dto

import jakarta.validation.constraints.Min
import java.time.LocalDate

data class DayRequest(
    @field:Min(1)
    val dayNumber: Int,
    val dayDate: LocalDate? = null,
    val notes: String? = null,
)

data class DayResponse(
    val id: Long,
    val tripId: Long,
    val dayNumber: Int,
    val dayDate: LocalDate?,
    val notes: String?,
)