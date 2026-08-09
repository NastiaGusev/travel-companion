package com.example.travel.controller

import com.example.travel.dto.DayRequest
import com.example.travel.dto.DayResponse
import com.example.travel.service.ItineraryDayService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ItineraryDayController(
    private val dayService: ItineraryDayService,
) {
    @PostMapping("/trips/{tripId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: Long,
        @PathVariable tripId: Long,
        @Valid @RequestBody request: DayRequest,
    ): DayResponse = dayService.create(userId, tripId, request)

    @GetMapping("/trips/{tripId}/days")
    fun list(
        @AuthenticationPrincipal userId: Long,
        @PathVariable tripId: Long,
    ): List<DayResponse> = dayService.listForTrip(userId, tripId)

    @DeleteMapping("/days/{dayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable dayId: Long,
    ) = dayService.delete(userId, dayId)
}