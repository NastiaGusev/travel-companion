package com.example.travel.model.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.util.UUID

data class TripRequest(
    @field:NotBlank
    val title: String,
    val destination: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val description: String? = null,
    val version: Long? = null,
)

data class TripResponse(
    val id: UUID,
    val title: String,
    val destination: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val description: String?,
    val version: Long,
)