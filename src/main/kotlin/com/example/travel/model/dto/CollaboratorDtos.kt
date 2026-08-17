package com.example.travel.model.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AddCollaboratorRequest(
    @field:NotBlank
    @field:Email
    val email: String,
)

data class CollaboratorResponse(
    val userId: UUID,
    val email: String,
    val role: String,
)