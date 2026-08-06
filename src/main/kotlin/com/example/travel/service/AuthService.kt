package com.example.travel.service

import com.example.travel.dto.LoginRequest
import com.example.travel.dto.RegisterRequest
import com.example.travel.entity.User
import com.example.travel.repository.UserRepository
import com.example.travel.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    fun register(request: RegisterRequest): String {
        if (userRepository.findByEmail(request.email) != null) {
            throw IllegalArgumentException("Email already registered")
        }
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password) ?: "",
            displayName = request.displayName,
        )
        val saved = userRepository.save(user)
        return jwtService.generateToken(saved.id!!, saved.email)
    }

    fun login(request: LoginRequest): String {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }
        return jwtService.generateToken(user.id!!, user.email)
    }
}