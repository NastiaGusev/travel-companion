package com.example.travel.dto

import java.time.LocalDate

data class DayRequest(
    val notes: String? = null,
)

data class DayResponse(
    val id: Long,
    val tripId: Long,
    val dayNumber: Int,
    val dayDate: LocalDate?,
    val notes: String?,
)