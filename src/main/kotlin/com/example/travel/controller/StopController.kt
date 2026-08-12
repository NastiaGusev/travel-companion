package com.example.travel.controller

import com.example.travel.model.dto.StopRequest
import com.example.travel.model.dto.StopResponse
import com.example.travel.service.StopService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Stops", description = "Manage the stops within a day; stops are ordered automatically by start time")
@RestController
@RequestMapping("/api")
class StopController(
    private val stopService: StopService,
) {
    @Operation(
        summary = "Add a stop to a day",
        description = "Creates a new stop within the day. Stops are ordered automatically by start time, with untimed stops placed last.",
    )
    @PostMapping("/days/{dayId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
        @Valid @RequestBody request: StopRequest,
    ): StopResponse = stopService.create(userId, dayId, request)

    @Operation(
        summary = "List stops of a day",
        description = "Returns all stops for the day, ordered by their position.",
    )
    @GetMapping("/days/{dayId}/stops")
    fun list(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
    ): List<StopResponse> = stopService.listForDay(userId, dayId)

    @Operation(
        summary = "Update a stop",
        description = "Updates a stop's details. If its start time changes, the day's stops are reordered automatically.",
    )
    @PutMapping("/stops/{stopId}")
    fun update(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable stopId: UUID,
        @Valid @RequestBody request: StopRequest,
    ): StopResponse = stopService.update(userId, stopId, request)

    @Operation(
        summary = "Delete a stop",
        description = "Removes a stop and resequences the remaining stops. Returns the updated list of stops.",
    )
    @DeleteMapping("/stops/{stopId}")
    fun delete(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable stopId: UUID,
    ): List<StopResponse> = stopService.delete(userId, stopId)
}