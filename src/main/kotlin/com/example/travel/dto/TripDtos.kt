package com.example.travel.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class TripRequest(
    @field:NotBlank
    val title: String,
    val destination: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val description: String? = null,
)

data class TripResponse(
    val id: Long,
    val title: String,
    val destination: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val description: String?,
)