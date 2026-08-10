package com.example.travel.controller

import com.example.travel.model.dto.DayRequest
import com.example.travel.model.dto.DayResponse
import com.example.travel.model.dto.SwapDaysRequest
import com.example.travel.service.ItineraryDayService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class ItineraryDayController(
    private val dayService: ItineraryDayService,
) {
    @PostMapping("/trips/{tripId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: DayRequest,
    ): DayResponse = dayService.create(userId, tripId, request)

    @GetMapping("/trips/{tripId}/days")
    fun list(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
    ): List<DayResponse> = dayService.listForTrip(userId, tripId)

    @DeleteMapping("/days/{dayId}")
    fun delete(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
    ): List<DayResponse> = dayService.delete(userId, dayId)

    @PutMapping("/days/{dayId}")
    fun updateNotes(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
        @Valid @RequestBody request: DayRequest,
    ): DayResponse = dayService.updateNotes(userId, dayId, request.notes)

    @PostMapping("/trips/{tripId}/days/swap")
    fun swap(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: SwapDaysRequest,
    ): List<DayResponse> = dayService.swapDays(userId, tripId, request.dayIdA, request.dayIdB)
}