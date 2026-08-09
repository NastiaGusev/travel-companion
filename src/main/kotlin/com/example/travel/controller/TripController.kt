package com.example.travel.controller

import com.example.travel.dto.TripRequest
import com.example.travel.dto.TripResponse
import com.example.travel.service.TripService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trips")
class TripController(
    private val tripService: TripService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: TripRequest,
    ): TripResponse = tripService.create(userId, request)

    @GetMapping
    fun list(@AuthenticationPrincipal userId: Long): List<TripResponse> =
        tripService.listForUser(userId)

    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): TripResponse = tripService.getForUser(id, userId)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ) = tripService.deleteForUser(id, userId)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: TripRequest,
    ): TripResponse = tripService.update(userId, id, request)
}