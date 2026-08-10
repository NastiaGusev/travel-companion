package com.example.travel.support

import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
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

fun TestRestTemplate.createTrip(
    token: String,
    title: String = "Trip",
    startDate: String? = "2026-05-01",
    endDate: String? = "2026-05-10",
): Map<*, *> {
    val entity = HttpEntity(
        mapOf("title" to title, "startDate" to startDate, "endDate" to endDate),
        bearerHeaders(token),
    )
    return exchange("/api/trips", HttpMethod.POST, entity, Map::class.java).body!!
}

/** Creates a day under a trip and returns its response body (id under "id"). */
fun TestRestTemplate.createDay(token: String, tripId: Any, notes: String? = null): Map<*, *> {
    val entity = HttpEntity(mapOf("notes" to notes), bearerHeaders(token))
    return exchange("/api/trips/$tripId/days", HttpMethod.POST, entity, Map::class.java).body!!
}