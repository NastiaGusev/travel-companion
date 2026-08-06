package com.example.travel.service

import com.example.travel.dto.RegisterRequest
import com.example.travel.entity.User
import com.example.travel.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(request: RegisterRequest): User {
        if (userRepository.findByEmail(request.email) != null) {
            throw IllegalArgumentException("Email already registered")
        }
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password) ?: "",
            displayName = request.displayName,
        )
        return userRepository.save(user)
    }
}