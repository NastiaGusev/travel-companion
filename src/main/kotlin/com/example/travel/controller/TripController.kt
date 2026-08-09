package com.example.travel.controller

import com.example.travel.dto.TripRequest
import com.example.travel.dto.TripResponse
import com.example.travel.service.TripService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/trips")
class TripController(
    private val tripService: TripService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: TripRequest,
    ): TripResponse = tripService.create(userId, request)

    @GetMapping
    fun list(@AuthenticationPrincipal userId: UUID): List<TripResponse> =
        tripService.listForUser(userId)

    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
    ): TripResponse = tripService.getForUser(id, userId)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
    ) = tripService.deleteForUser(id, userId)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: TripRequest,
    ): TripResponse = tripService.update(userId, id, request)
}