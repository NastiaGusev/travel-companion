package com.example.travel.day

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
import java.util.*

class ItineraryDayIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var rest: TestRestTemplate

    // ---- helpers ----

    private fun listDays(token: String, tripId: Any) =
        rest.exchange<List<Map<*, *>>>(
            "/api/trips/$tripId/days", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(token)),
        )

    // ---- happy path: create, auto-number, derive date ----

    @Test
    fun `create returns 201 with day number 1 and the derived date`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!

        val entity = HttpEntity(mapOf("notes" to "Arrival"), bearerHeaders(token))
        val response = rest.exchange("/api/trips/$tripId/days", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.get("dayNumber")).isEqualTo(1)
        assertThat(response.body?.get("dayDate")).isEqualTo("2026-05-01")
        assertThat(response.body?.get("tripId")).isEqualTo(tripId)
        assertThat(response.body?.get("notes")).isEqualTo("Arrival")
    }

    @Test
    fun `second created day gets day number 2 and the next date`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!

        rest.createDay(token, tripId, "first")
        val second = rest.createDay(token, tripId, "second")

        assertThat(second["dayNumber"]).isEqualTo(2)
        assertThat(second["dayDate"]).isEqualTo("2026-05-02")
    }

    @Test
    fun `list returns days ordered by day number`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        rest.createDay(token, tripId, "one")
        rest.createDay(token, tripId, "two")
        rest.createDay(token, tripId, "three")

        val response = listDays(token, tripId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.map { it["dayNumber"] }).containsExactly(1, 2, 3)
    }

    @Test
    fun `update notes returns 200 with the new notes`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        val dayId = rest.createDay(token, tripId, "old")["id"]!!

        val entity = HttpEntity(mapOf("notes" to "updated"), bearerHeaders(token))
        val response = rest.exchange("/api/days/$dayId", HttpMethod.PUT, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("notes")).isEqualTo("updated")
    }

    // ---- delete closes the gap and renumbers (returns 200 + remaining days) ----

    @Test
    fun `delete a middle day renumbers the rest and returns them`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        rest.createDay(token, tripId, "first")
        val secondId = rest.createDay(token, tripId, "second")["id"]!!
        rest.createDay(token, tripId, "third")

        val response = rest.exchange<List<Map<*, *>>>(
            "/api/days/$secondId", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(token)),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val days = response.body!!
        assertThat(days).hasSize(2)
        assertThat(days.map { it["dayNumber"] }).containsExactly(1, 2)

        // the day formerly numbered 3 ("third") shifts down to 2, with its date shifted too
        val third = days.first { it["notes"] == "third" }
        assertThat(third["dayNumber"]).isEqualTo(2)
        assertThat(third["dayDate"]).isEqualTo("2026-05-02")
    }

    // ---- swap ----

    @Test
    fun `swap exchanges two day numbers`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        val firstId = rest.createDay(token, tripId, "first")["id"]!!
        val secondId = rest.createDay(token, tripId, "second")["id"]!!

        val entity = HttpEntity(
            mapOf("dayIdA" to firstId, "dayIdB" to secondId),
            bearerHeaders(token),
        )
        val response = rest.exchange<List<Map<*, *>>>(
            "/api/trips/$tripId/days/swap", HttpMethod.POST, entity,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val days = response.body!!
        // "second" now sits at day 1, "first" at day 2
        assertThat(days.first { it["dayNumber"] == 1 }["notes"]).isEqualTo("second")
        assertThat(days.first { it["dayNumber"] == 2 }["notes"]).isEqualTo("first")
    }

    // ---- validation (400) ----

    @Test
    fun `creating more days than the trip length returns 400`() {
        val token = rest.registerAndGetToken()
        // 2-day trip: May 1 to May 2
        val tripId = rest.createTrip(token, startDate = "2026-05-01", endDate = "2026-05-02")["id"]!!

        rest.createDay(token, tripId)   // day 1
        rest.createDay(token, tripId)   // day 2 — trip is now full

        val entity = HttpEntity(mapOf("notes" to null), bearerHeaders(token))
        val response = rest.exchange("/api/trips/$tripId/days", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `swapping days from different trips returns 400`() {
        val token = rest.registerAndGetToken()
        val tripId1 = rest.createTrip(token)["id"]!!
        val tripId2 = rest.createTrip(token)["id"]!!
        val dayInTrip1 = rest.createDay(token, tripId1, "t1")["id"]!!
        val dayInTrip2 = rest.createDay(token, tripId2, "t2")["id"]!!

        val entity = HttpEntity(
            mapOf("dayIdA" to dayInTrip1, "dayIdB" to dayInTrip2),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips/$tripId1/days/swap", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    // ---- auth (401) ----

    @Test
    fun `create without a token returns 401`() {
        val response = rest.postForEntity(
            "/api/trips/${UUID.randomUUID()}/days",
            mapOf("notes" to "x"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `list without a token returns 401`() {
        val response = rest.getForEntity("/api/trips/${UUID.randomUUID()}/days", Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ---- not found / ownership (404) ----

    @Test
    fun `create on an unknown trip returns 404`() {
        val token = rest.registerAndGetToken()
        val entity = HttpEntity(mapOf("notes" to "x"), bearerHeaders(token))
        val response = rest.exchange(
            "/api/trips/${UUID.randomUUID()}/days", HttpMethod.POST, entity, Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create on another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val tripId = rest.createTrip(tokenA)["id"]!!

        val entity = HttpEntity(mapOf("notes" to "x"), bearerHeaders(tokenB))
        val response = rest.exchange("/api/trips/$tripId/days", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `list on another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val tripId = rest.createTrip(tokenA)["id"]!!

        val response = rest.exchange<String>(
            "/api/trips/$tripId/days", HttpMethod.GET,
            HttpEntity<Void>(bearerHeaders(tokenB)),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete an unknown day returns 404`() {
        val token = rest.registerAndGetToken()
        val response = rest.exchange<Map<*, *>>(
            "/api/days/${UUID.randomUUID()}", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(token)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete a day on another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val tripId = rest.createTrip(tokenA)["id"]!!
        val dayId = rest.createDay(tokenA, tripId, "a")["id"]!!

        val response = rest.exchange<Map<*, *>>(
            "/api/days/$dayId", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(tokenB)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `update notes on another users day returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val tripId = rest.createTrip(tokenA)["id"]!!
        val dayId = rest.createDay(tokenA, tripId, "a")["id"]!!

        val entity = HttpEntity(mapOf("notes" to "hijack"), bearerHeaders(tokenB))
        val response = rest.exchange("/api/days/$dayId", HttpMethod.PUT, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `swap with an unknown day returns 404`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!
        val realDay = rest.createDay(token, tripId, "a")["id"]!!

        val entity = HttpEntity(
            mapOf("dayIdA" to realDay, "dayIdB" to UUID.randomUUID().toString()),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips/$tripId/days/swap", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}