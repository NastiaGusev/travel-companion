package com.example.travel.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email(message = "must be a valid email")
    @field:NotBlank
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, message = "password must be at least 8 characters")
    val password: String,

    val displayName: String? = null,
)

data class AuthResponse(
    val token: String,
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String,
)