package com.example.travel.collaborator

import com.example.travel.support.IntegrationTestBase
import com.example.travel.support.bearerHeaders
import com.example.travel.support.createDay
import com.example.travel.support.createTrip
import com.example.travel.support.registerAndGetToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class CollaboratorIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var rest: TestRestTemplate

    // ---- helpers ----

    private data class Shared(
        val ownerToken: String,
        val editorToken: String,
        val editorEmail: String,
        val tripId: Any,
    )

    /** Registers an owner and an editor, creates a trip, shares it with the editor. */
    private fun sharedTrip(): Shared {
        val ownerToken = rest.registerAndGetToken()
        val editorEmail = "editor-${UUID.randomUUID()}@example.com"
        val editorToken = rest.registerAndGetToken(email = editorEmail)
        val tripId = rest.createTrip(ownerToken)["id"]!!
        addCollaborator(ownerToken, tripId, editorEmail)   // 201 expected
        return Shared(ownerToken, editorToken, editorEmail, tripId)
    }

    private fun addCollaborator(token: String, tripId: Any, email: String) =
        rest.exchange<String>(
            "/api/trips/$tripId/collaborators",
            HttpMethod.POST,
            HttpEntity(mapOf("email" to email), bearerHeaders(token)),
        )

    private fun listCollaborators(token: String, tripId: Any) =
        rest.exchange<List<Map<*, *>>>(
            "/api/trips/$tripId/collaborators",
            HttpMethod.GET,
            HttpEntity<Void>(bearerHeaders(token)),
        )

    private fun listDays(token: String, tripId: Any) =
        rest.exchange<String>(
            "/api/trips/$tripId/days",
            HttpMethod.GET,
            HttpEntity<Void>(bearerHeaders(token)),
        )

    // ---- collaborator management ----

    @Test
    fun `owner adds an editor and the list contains owner and editor`() {
        val ownerToken = rest.registerAndGetToken()
        val editorEmail = "editor-${UUID.randomUUID()}@example.com"
        rest.registerAndGetToken(email = editorEmail)
        val tripId = rest.createTrip(ownerToken)["id"]!!

        val addResponse = addCollaborator(ownerToken, tripId, editorEmail)
        assertThat(addResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val list = listCollaborators(ownerToken, tripId)
        assertThat(list.statusCode).isEqualTo(HttpStatus.OK)
        val roles = list.body!!.map { it["role"] }
        assertThat(roles).contains("OWNER", "EDITOR")
        assertThat(list.body!!.map { it["email"] }).contains(editorEmail)
    }

    @Test
    fun `adding a non-existent email returns 404`() {
        val ownerToken = rest.registerAndGetToken()
        val tripId = rest.createTrip(ownerToken)["id"]!!

        val response = addCollaborator(ownerToken, tripId, "nobody-${UUID.randomUUID()}@example.com")
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `adding the same editor twice returns 400`() {
        val s = sharedTrip()
        val response = addCollaborator(s.ownerToken, s.tripId, s.editorEmail)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `owner adding themselves returns 400`() {
        val ownerEmail = "owner-${UUID.randomUUID()}@example.com"
        val token = rest.registerAndGetToken(email = ownerEmail)
        val tripId = rest.createTrip(token)["id"]!!

        val response = addCollaborator(token, tripId, ownerEmail)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `editor cannot add another collaborator`() {
        val s = sharedTrip()
        val thirdEmail = "third-${UUID.randomUUID()}@example.com"
        rest.registerAndGetToken(email = thirdEmail)

        val response = addCollaborator(s.editorToken, s.tripId, thirdEmail)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `non-collaborator cannot list collaborators`() {
        val s = sharedTrip()
        val outsiderToken = rest.registerAndGetToken()

        val response = rest.exchange<String>(
            "/api/trips/${s.tripId}/collaborators",
            HttpMethod.GET,
            HttpEntity<Void>(bearerHeaders(outsiderToken)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `owner removes an editor and the editor loses access`() {
        val s = sharedTrip()

        // find the editor's userId from the collaborators list
        val list = listCollaborators(s.ownerToken, s.tripId)
        val editorEntry = list.body!!.first { it["role"] == "EDITOR" }
        val editorUserId = editorEntry["userId"]

        val removeResponse = rest.exchange<Void>(
            "/api/trips/${s.tripId}/collaborators/$editorUserId",
            HttpMethod.DELETE,
            HttpEntity<Void>(bearerHeaders(s.ownerToken)),
        )
        assertThat(removeResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // editor can no longer see the trip's days
        assertThat(listDays(s.editorToken, s.tripId).statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---- editor access (the core proof) ----

    @Test
    fun `editor can list days on the shared trip`() {
        val s = sharedTrip()
        rest.createDay(s.ownerToken, s.tripId)   // owner adds a day

        val response = rest.exchange<List<Map<*, *>>>(
            "/api/trips/${s.tripId}/days",
            HttpMethod.GET,
            HttpEntity<Void>(bearerHeaders(s.editorToken)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `editor can create a day on the shared trip`() {
        val s = sharedTrip()
        val response = rest.exchange(
            "/api/trips/${s.tripId}/days",
            HttpMethod.POST,
            HttpEntity(mapOf("notes" to "editor's day"), bearerHeaders(s.editorToken)),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `editor can create a stop on the shared trip`() {
        val s = sharedTrip()
        val dayId = rest.createDay(s.editorToken, s.tripId)["id"]!!

        val response = rest.exchange(
            "/api/days/$dayId/stops",
            HttpMethod.POST,
            HttpEntity(mapOf("name" to "Museum", "startTime" to "10:00:00"), bearerHeaders(s.editorToken)),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `editor can update trip details`() {
        val s = sharedTrip()
        val response = rest.exchange(
            "/api/trips/${s.tripId}",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "Edited by editor", "startDate" to "2026-05-01", "endDate" to "2026-05-10"),
                bearerHeaders(s.editorToken),
            ),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("title")).isEqualTo("Edited by editor")
    }

    @Test
    fun `editor cannot delete the trip`() {
        val s = sharedTrip()
        val response = rest.exchange<String>(
            "/api/trips/${s.tripId}",
            HttpMethod.DELETE,
            HttpEntity<Void>(bearerHeaders(s.editorToken)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `shared trip appears in the editor's trip list`() {
        val s = sharedTrip()
        val response = rest.exchange<List<Map<*, *>>>(
            "/api/trips",
            HttpMethod.GET,
            HttpEntity<Void>(bearerHeaders(s.editorToken)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.map { it["id"] }).contains(s.tripId)
    }

    // ---- regression: outsider has no access ----

    @Test
    fun `non-collaborator cannot list the trip's days`() {
        val s = sharedTrip()
        val outsiderToken = rest.registerAndGetToken()
        assertThat(listDays(outsiderToken, s.tripId).statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}