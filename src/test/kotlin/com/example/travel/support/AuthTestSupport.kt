package com.example.travel.support

import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpHeaders
import java.util.UUID

/**
 * Registers a fresh user and returns a valid bearer token.
 * Each call uses a unique email so tests never collide on the users table.
 */
fun TestRestTemplate.registerAndGetToken(
    email: String = "user-${UUID.randomUUID()}@example.com",
    password: String = "supersecret",
): String {
    val body = mapOf("email" to email, "password" to password, "displayName" to "Test")
    val response = postForEntity("/api/auth/register", body, Map::class.java)
    return response.body?.get("token") as String
}

fun bearerHeaders(token: String) = HttpHeaders().apply { setBearerAuth(token) }