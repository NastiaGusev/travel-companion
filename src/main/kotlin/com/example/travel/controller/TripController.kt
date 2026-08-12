package com.example.travel.controller

import com.example.travel.model.dto.TripRequest
import com.example.travel.model.dto.TripResponse
import com.example.travel.service.TripService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Trips", description = "Create, view, update, and delete trips owned by the authenticated user")
@RestController
@RequestMapping("/api/trips")
class TripController(
    private val tripService: TripService,
) {
    @Operation(
        summary = "Create a trip",
        description = "Creates a new trip owned by the authenticated user."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: TripRequest,
    ): TripResponse = tripService.create(userId, request)

    @Operation(
        summary = "List trips",
        description = "Returns all trips owned by the authenticated user.",
    )
    @GetMapping
    fun list(@AuthenticationPrincipal userId: UUID): List<TripResponse> =
        tripService.listForUser(userId)

    @Operation(
        summary = "Get a trip",
        description = "Returns a single trip by ID. Trips owned by other users are indistinguishable from non-existent ones.",
    )
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
    ): TripResponse = tripService.getForUser(id, userId)

    @Operation(
        summary = "Delete a trip",
        description = "Deletes a trip owned by the authenticated user.",
    )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
    ) = tripService.deleteForUser(id, userId)

    @Operation(
        summary = "Update a trip",
        description = "Replaces the details of a trip owned by the authenticated user.",
    )
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: TripRequest,
    ): TripResponse = tripService.update(userId, id, request)
}