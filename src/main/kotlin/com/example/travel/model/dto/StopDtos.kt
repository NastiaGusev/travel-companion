package com.example.travel.model.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalTime
import java.util.UUID

data class StopRequest(
    @field:NotBlank
    val name: String,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val notes: String? = null,
)

data class StopResponse(
    val id: UUID,
    val dayId: UUID,
    val name: String,
    val position: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val notes: String?,
)