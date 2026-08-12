package com.example.travel.controller

import com.example.travel.model.dto.AuthResponse
import com.example.travel.model.dto.LoginRequest
import com.example.travel.model.dto.RegisterRequest
import com.example.travel.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Authentication", description = "Register and log in to obtain a JWT")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(
        summary = "Register a new user",
        description = "Creates a new account and returns a JWT for the authenticated session.",
    )
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        AuthResponse(token = authService.register(request))

    @Operation(
        summary = "Log in",
        description = "Authenticates an existing user with email and password and returns a JWT.",
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        AuthResponse(token = authService.login(request))
}

