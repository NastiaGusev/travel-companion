package com.example.travel.controller

import com.example.travel.dto.AuthResponse
import com.example.travel.dto.LoginRequest
import com.example.travel.dto.RegisterRequest
import com.example.travel.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Map<String, Long>> {
        val user = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to user.id!!))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        AuthResponse(token = authService.login(request))
}

