package com.example.travel

import com.example.travel.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class AuthIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var rest: TestRestTemplate

    // unique email per test run so tests don't collide on the users table
    private fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

    private fun register(email: String, password: String = "supersecret"): Map<*, *>? {
        val body = mapOf("email" to email, "password" to password, "displayName" to "Test")
        return rest.postForEntity("/api/auth/register", body, Map::class.java).body
    }

    @Test
    fun `register returns a token`() {
        val response = rest.postForEntity(
            "/api/auth/register",
            mapOf("email" to uniqueEmail(), "password" to "supersecret"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.get("token")).isNotNull()
    }

    @Test
    fun `duplicate email is rejected with 409`() {
        val email = uniqueEmail()
        register(email)
        val response = rest.postForEntity(
            "/api/auth/register",
            mapOf("email" to email, "password" to "supersecret"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `short password is rejected with 400`() {
        val response = rest.postForEntity(
            "/api/auth/register",
            mapOf("email" to uniqueEmail(), "password" to "short"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `login returns a token for valid credentials`() {
        val email = uniqueEmail()
        register(email, "supersecret")
        val response = rest.postForEntity(
            "/api/auth/login",
            mapOf("email" to email, "password" to "supersecret"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("token")).isNotNull()
    }

    @Test
    fun `login with wrong password returns 401`() {
        val email = uniqueEmail()
        register(email, "supersecret")
        val response = rest.postForEntity(
            "/api/auth/login",
            mapOf("email" to email, "password" to "wrongpassword"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `protected route without a token returns 401`() {
        val response = rest.getForEntity<String>("/api/trips")
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `protected route with a valid token returns 200`() {
        val email = uniqueEmail()
        val token = register(email)?.get("token") as String

        val headers = HttpHeaders().apply { setBearerAuth(token) }
        val response = rest.exchange<String>(
            "/api/trips", HttpMethod.GET, HttpEntity<Void>(headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `tampered token is rejected with 401`() {
        val email = uniqueEmail()
        val token = register(email)?.get("token") as String
        // corrupt the signature (last segment) so verification fails
        val tampered = token.dropLast(2) + "xy"

        val headers = HttpHeaders().apply { setBearerAuth(tampered) }
        val response = rest.exchange<String>(
            "/api/trips", HttpMethod.GET, HttpEntity<Void>(headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}