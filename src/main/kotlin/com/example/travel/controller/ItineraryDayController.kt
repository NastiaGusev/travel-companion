package com.example.travel.controller

import com.example.travel.model.dto.DayRequest
import com.example.travel.model.dto.DayResponse
import com.example.travel.model.dto.SwapDaysRequest
import com.example.travel.service.ItineraryDayService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Itinerary days", description = "Manage the days within a trip; days are auto-numbered and their dates derive from the trip's start date")
@RestController
@RequestMapping("/api")
class ItineraryDayController(
    private val dayService: ItineraryDayService,
) {
    @Operation(
        summary = "Add a day to a trip",
        description = "Appends a new day to the trip. The day number is assigned automatically as the next in sequence.",
    )
    @PostMapping("/trips/{tripId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: DayRequest,
    ): DayResponse = dayService.create(userId, tripId, request)

    @Operation(
        summary = "List days of a trip",
        description = "Returns all days of the trip, ordered by day number.",
    )
    @GetMapping("/trips/{tripId}/days")
    fun list(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
    ): List<DayResponse> = dayService.listForTrip(userId, tripId)

    @Operation(
        summary = "Delete a day",
        description = "Removes a day and renumbers the remaining days to close the gap. Returns the updated list of days.",
    )
    @DeleteMapping("/days/{dayId}")
    fun delete(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
    ): List<DayResponse> = dayService.delete(userId, dayId)

    @Operation(
        summary = "Update a day's notes",
        description = "Updates the notes for a single day.",
    )
    @PutMapping("/days/{dayId}")
    fun updateNotes(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
        @Valid @RequestBody request: DayRequest,
    ): DayResponse = dayService.updateNotes(userId, dayId, request)

    @Operation(
        summary = "Swap two days",
        description = "Exchanges the positions of two days within the same trip.",
    )
    @PostMapping("/trips/{tripId}/days/swap")
    fun swap(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: SwapDaysRequest,
    ): List<DayResponse> = dayService.swapDays(userId, tripId, request.dayIdA, request.dayIdB)
}