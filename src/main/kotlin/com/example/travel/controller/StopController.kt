package com.example.travel.controller

import com.example.travel.model.dto.StopRequest
import com.example.travel.model.dto.StopResponse
import com.example.travel.service.StopService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class StopController(
    private val stopService: StopService,
) {
    @PostMapping("/days/{dayId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
        @Valid @RequestBody request: StopRequest,
    ): StopResponse = stopService.create(userId, dayId, request)

    @GetMapping("/days/{dayId}/stops")
    fun list(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable dayId: UUID,
    ): List<StopResponse> = stopService.listForDay(userId, dayId)

    @PutMapping("/stops/{stopId}")
    fun update(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable stopId: UUID,
        @Valid @RequestBody request: StopRequest,
    ): StopResponse = stopService.update(userId, stopId, request)

    @DeleteMapping("/stops/{stopId}")
    fun delete(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable stopId: UUID,
    ): List<StopResponse> = stopService.delete(userId, stopId)
}