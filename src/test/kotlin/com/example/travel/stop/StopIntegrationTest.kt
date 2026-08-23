package com.example.travel.stop

import com.example.travel.support.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.*

class StopIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var rest: TestRestTemplate

    // ---- helpers ----

    /** Registers a user, creates a trip + one day, returns (token, dayId). */
    private fun setupDay(): Pair<String, Any> {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        val dayId = rest.createDay(token, tripId)["id"]!!
        return token to dayId
    }

    private fun createStop(
        token: String,
        dayId: Any,
        title: String,
        startTime: String? = null,
        endTime: String? = null,
        notes: String? = null,
    ): Map<*, *>? {
        val entity = HttpEntity(
            mapOf("title" to title, "startTime" to startTime, "endTime" to endTime, "notes" to notes),
            bearerHeaders(token),
        )
        return rest.exchange("/api/days/$dayId/stops", HttpMethod.POST, entity, Map::class.java).body
    }

    private fun listStops(token: String, dayId: Any) =
        rest.exchange<List<Map<*, *>>>(
            "/api/days/$dayId/stops", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(token)),
        )

    // ---- happy path: create + time-driven ordering ----

    @Test
    fun `create returns 201 with position 1 for the first stop`() {
        val (token, dayId) = setupDay()

        val entity = HttpEntity(
            mapOf("title" to "Museum", "startTime" to "10:00:00"),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/days/$dayId/stops", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.get("position")).isEqualTo(1)
        assertThat(response.body?.get("title")).isEqualTo("Museum")
        assertThat(response.body?.get("dayId")).isEqualTo(dayId)
    }

    @Test
    fun `stops are ordered by start time regardless of insertion order`() {
        val (token, dayId) = setupDay()

        // insert out of chronological order
        createStop(token, dayId, "Lunch", startTime = "13:00:00")
        createStop(token, dayId, "Breakfast", startTime = "08:00:00")
        createStop(token, dayId, "Dinner", startTime = "19:00:00")

        val response = listStops(token, dayId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val byPosition = response.body!!.sortedBy { it["position"] as Int }
        assertThat(byPosition.map { it["title"] })
            .containsExactly("Breakfast", "Lunch", "Dinner")
        assertThat(byPosition.map { it["position"] }).containsExactly(1, 2, 3)
    }

    @Test
    fun `stops without a start time sort last`() {
        val (token, dayId) = setupDay()

        createStop(token, dayId, "Timed", startTime = "09:00:00")
        createStop(token, dayId, "Untimed")   // null start time → sinks to the end

        val response = listStops(token, dayId)

        val byPosition = response.body!!.sortedBy { it["position"] as Int }
        assertThat(byPosition.map { it["title"] }).containsExactly("Timed", "Untimed")
    }

    @Test
    fun `inserting an earlier stop renumbers the later ones`() {
        val (token, dayId) = setupDay()

        createStop(token, dayId, "Noon", startTime = "12:00:00")   // position 1
        createStop(token, dayId, "Morning", startTime = "09:00:00") // should slot in front

        val response = listStops(token, dayId)
        val byPosition = response.body!!.sortedBy { it["position"] as Int }

        assertThat(byPosition.first { it["title"] == "Morning" }["position"]).isEqualTo(1)
        assertThat(byPosition.first { it["title"] == "Noon" }["position"]).isEqualTo(2)
    }

    // ---- update re-triggers ordering when time changes ----

    @Test
    fun `updating a stops time reorders the day`() {
        val (token, dayId) = setupDay()

        val lateId = createStop(token, dayId, "Wanderer", startTime = "18:00:00")?.get("id")!!
        createStop(token, dayId, "Anchor", startTime = "10:00:00")
        // "Wanderer" is currently position 2 (18:00 after 10:00). Move it to 06:00 → should become position 1.

        val entity = HttpEntity(
            mapOf("title" to "Wanderer", "startTime" to "06:00:00"),
            bearerHeaders(token),
        )
        val updated = rest.exchange("/api/stops/$lateId", HttpMethod.PUT, entity, Map::class.java)

        assertThat(updated.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(updated.body?.get("position")).isEqualTo(1)
    }

    // ---- delete closes the gap and returns remaining stops ----

    @Test
    fun `delete a middle stop resequences and returns the remaining stops`() {
        val (token, dayId) = setupDay()

        createStop(token, dayId, "A", startTime = "08:00:00")
        val middleId = createStop(token, dayId, "B", startTime = "12:00:00")?.get("id")!!
        createStop(token, dayId, "C", startTime = "16:00:00")

        val response = rest.exchange<List<Map<*, *>>>(
            "/api/stops/$middleId", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(token)),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val stops = response.body!!.sortedBy { it["position"] as Int }
        assertThat(stops.map { it["title"] }).containsExactly("A", "C")
        assertThat(stops.map { it["position"] }).containsExactly(1, 2)
    }

    // ---- validation (400) ----

    @Test
    fun `create with a blank title returns 400`() {
        val (token, dayId) = setupDay()
        val entity = HttpEntity(mapOf("title" to "", "startTime" to "10:00:00"), bearerHeaders(token))
        val response = rest.exchange("/api/days/$dayId/stops", HttpMethod.POST, entity, Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `create with end time not after start time returns 400`() {
        val (token, dayId) = setupDay()
        val entity = HttpEntity(
            mapOf("title" to "Bad", "startTime" to "10:00:00", "endTime" to "09:00:00"),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/days/$dayId/stops", HttpMethod.POST, entity, Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    // ---- auth (401) ----

    @Test
    fun `create without a token returns 401`() {
        val response = rest.postForEntity(
            "/api/days/${UUID.randomUUID()}/stops",
            mapOf("title" to "x"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `list without a token returns 401`() {
        val response = rest.getForEntity("/api/days/${UUID.randomUUID()}/stops", Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ---- not found / ownership (404) ----
    // note: error responses decode as String, since the success body here is a list/object mismatch

    @Test
    fun `create on an unknown day returns 404`() {
        val token = rest.registerAndGetToken()
        val entity = HttpEntity(mapOf("title" to "x"), bearerHeaders(token))
        val response = rest.exchange(
            "/api/days/${UUID.randomUUID()}/stops", HttpMethod.POST, entity, String::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create on another users day returns 404`() {
        val (_, dayId) = setupDay()
        val tokenB = rest.registerAndGetToken()

        val entity = HttpEntity(mapOf("title" to "x"), bearerHeaders(tokenB))
        val response = rest.exchange(
            "/api/days/$dayId/stops", HttpMethod.POST, entity, String::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `list on another users day returns 404`() {
        val (_, dayId) = setupDay()
        val tokenB = rest.registerAndGetToken()

        val response = rest.exchange<String>(
            "/api/days/$dayId/stops", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(tokenB)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `update a stop on another users day returns 404`() {
        val (tokenA, dayId) = setupDay()
        val stopId = createStop(tokenA, dayId, "A", startTime = "10:00:00")?.get("id")!!
        val tokenB = rest.registerAndGetToken()

        val entity = HttpEntity(mapOf("title" to "hijack"), bearerHeaders(tokenB))
        val response = rest.exchange<String>("/api/stops/$stopId", HttpMethod.PUT, entity)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `update with a blank title and no place returns 400`() {
        val (token, dayId) = setupDay()
        val stopId = createStop(token, dayId, title = "Original")?.get("id")!!

        val entity = HttpEntity(
            mapOf("title" to "", "startTime" to "10:00:00"),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/stops/$stopId", HttpMethod.PUT, entity, Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `delete a stop on another users day returns 404`() {
        val (tokenA, dayId) = setupDay()
        val stopId = createStop(tokenA, dayId, "A", startTime = "10:00:00")?.get("id")!!
        val tokenB = rest.registerAndGetToken()

        val response = rest.exchange<String>(
            "/api/stops/$stopId", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(tokenB)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete an unknown stop returns 404`() {
        val token = rest.registerAndGetToken()
        val response = rest.exchange<String>(
            "/api/stops/${UUID.randomUUID()}", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(token)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}