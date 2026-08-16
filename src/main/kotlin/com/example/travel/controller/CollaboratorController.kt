package com.example.travel.controller

import com.example.travel.model.dto.AddCollaboratorRequest
import com.example.travel.model.dto.CollaboratorResponse
import com.example.travel.service.CollaboratorService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Collaborators", description = "Manage editors on a trip (owner only for add/remove)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/trips/{tripId}/collaborators")
class CollaboratorController(
    private val collaboratorService: CollaboratorService,
) {
    @Operation(summary = "Add an editor", description = "Owner shares the trip with an existing user by email.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: AddCollaboratorRequest,
    ): CollaboratorResponse = collaboratorService.addEditor(tripId, userId, request.email)

    @Operation(summary = "List collaborators", description = "Owner or editor can view the trip's collaborators.")
    @GetMapping
    fun list(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
    ): List<CollaboratorResponse> = collaboratorService.listCollaborators(tripId, userId)

    @Operation(summary = "Remove an editor", description = "Owner removes a collaborator from the trip.")
    @DeleteMapping("/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @PathVariable targetUserId: UUID,
    ) = collaboratorService.removeEditor(tripId, userId, targetUserId)
}