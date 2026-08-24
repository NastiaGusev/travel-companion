package com.example.travel.model.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class QuickAddRequest(
    @field:NotBlank
    val text: String,
)

data class QuickAddResponse(
    val days: List<QuickAddDayResult>,
)

data class QuickAddDayResult(
    val dayId: UUID,
    val dayNumber: Int,
    val stops: List<StopResponse>,
)
