package com.example.travel.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MeController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: Long?): Map<String, Long?> =
        mapOf("userId" to userId)
}