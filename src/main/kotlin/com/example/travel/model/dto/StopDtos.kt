package com.example.travel.model.dto

import java.time.LocalTime
import java.util.*

data class StopRequest(
    val title: String?,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val notes: String? = null,
    val placeId: String? = null,
)

data class StopResponse(
    val id: UUID,
    val dayId: UUID,
    val title: String,
    val position: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val notes: String?,
    val place: PlaceDto?,
)

data class PlaceDto(
    val name: String?,
    val placeId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val category: String?,
)