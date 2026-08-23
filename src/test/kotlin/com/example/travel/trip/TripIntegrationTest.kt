package com.example.travel.trip

import com.example.travel.support.IntegrationTestBase
import com.example.travel.support.bearerHeaders
import com.example.travel.support.createTrip
import com.example.travel.support.registerAndGetToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.UUID
import org.springframework.boot.resttestclient.exchange
import kotlin.collections.get

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TripIntegrationTest: IntegrationTestBase() {

    @Autowired
    lateinit var rest: TestRestTemplate

    // ---- helpers ----

    private fun createTrip(
        token: String,
        title: String = "Japan 2026",
        destinationName: String? = "Tokyo",
        startDate: LocalDate? = LocalDate.of(2026, 5, 1),
        endDate: LocalDate? = LocalDate.of(2026, 5, 10),
        description: String? = "Cherry blossoms",
    ): Map<*, *>? {
        val body = mapOf(
            "title" to title,
            "destinationName" to destinationName,
            "startDate" to startDate?.toString(),
            "endDate" to endDate?.toString(),
            "description" to description,
        )
        val entity = HttpEntity(body, bearerHeaders(token))
        return rest.exchange("/api/trips", HttpMethod.POST, entity, Map::class.java).body
    }

    private fun get(token: String, id: Any) =
        rest.exchange<Map<*, *>>(
            "/api/trips/$id", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(token)),
        )

    @Suppress("UNCHECKED_CAST")
    private fun destinationOf(body: Map<*, *>?): Map<String, Any?>? =
        body?.get("destination") as? Map<String, Any?>

    // ---- happy path: full CRUD cycle ----

    @Test
    fun `create returns 201 with an id and the submitted fields`() {
        val token = rest.registerAndGetToken()
        val entity = HttpEntity(
            mapOf(
                "title" to "Japan 2026",
                "startDate" to "2026-05-01",
                "endDate" to "2026-05-10",
                "description" to "Cherry blossoms",
                "destinationName" to "Tokyo",
            ),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.get("id")).isNotNull()
        assertThat(response.body?.get("title")).isEqualTo("Japan 2026")
        assertThat(destinationOf(response.body)?.get("name")).isEqualTo("Tokyo")
    }

    @Test
    fun `create without a destination has no anchor`() {
        val token = rest.registerAndGetToken()
        val entity = HttpEntity(
            mapOf("title" to "No dest", "startDate" to "2026-05-01", "endDate" to "2026-05-10"),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.get("destination")).isNull()
    }

    @Test
    fun `get returns 200 with the created trip`() {
        val token = rest.registerAndGetToken()
        val id = createTrip(token)?.get("id")!!

        val response = get(token, id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("id")).isEqualTo(id)
        assertThat(response.body?.get("title")).isEqualTo("Japan 2026")
    }

    @Test
    fun `list returns 200 containing the created trip`() {
        val token = rest.registerAndGetToken()
        createTrip(token, title = "Trip A")

        val response = rest.exchange<List<Map<*, *>>>(
            "/api/trips", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(token)),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        assertThat(response.body!!.map { it["title"] }).contains("Trip A")
    }

    @Test
    fun `list only returns the callers own trips`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        createTrip(tokenA, title = "A's trip")

        val response = rest.exchange<List<Map<*, *>>>(
            "/api/trips", HttpMethod.GET, HttpEntity<Void>(bearerHeaders(tokenB)),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `update returns 200 with the changed fields`() {
        val token = rest.registerAndGetToken()
        val id = createTrip(token)?.get("id")!!

        val entity = HttpEntity(
            mapOf(
                "title" to "Japan (updated)",
                "destinationName" to "Kyoto",
                "startDate" to "2026-05-01",
                "endDate" to "2026-05-12",
                "description" to "Extended",
                "version" to 0,
            ),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips/$id", HttpMethod.PUT, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("title")).isEqualTo("Japan (updated)")
        assertThat(destinationOf(response.body)?.get("name")).isEqualTo("Kyoto")
    }

    @Test
    fun `delete returns 204 and the trip is then gone`() {
        val token = rest.registerAndGetToken()
        val id = createTrip(token)?.get("id")!!

        val deleteResponse = rest.exchange<Void>(
            "/api/trips/$id", HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders(token)),
        )
        assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val getResponse = get(token, id)
        assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---- validation (400) ----

    @Test
    fun `create with a blank title returns 400`() {
        val token = rest.registerAndGetToken()
        val entity = HttpEntity(
            mapOf("title" to "", "destinationName" to "Tokyo"),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `create with end date before start date returns 400`() {
        val token = rest.registerAndGetToken()
        val entity = HttpEntity(
            mapOf(
                "title" to "Backwards",
                "startDate" to "2026-05-10",
                "endDate" to "2026-05-01",
            ),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips", HttpMethod.POST, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    // ---- auth (401) ----

    @Test
    fun `create without a token returns 401`() {
        val response = rest.postForEntity(
            "/api/trips",
            mapOf("title" to "No auth"),
            Map::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `list without a token returns 401`() {
        val response = rest.getForEntity("/api/trips", Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ---- not found (404) ----

    @Test
    fun `get with an unknown id returns 404`() {
        val token = rest.registerAndGetToken()
        val response = get(token, UUID.randomUUID())
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete with an unknown id returns 404`() {
        val token = rest.registerAndGetToken()
        val response = rest.exchange<Map<*, *>>(
            "/api/trips/${UUID.randomUUID()}",
            HttpMethod.DELETE,
            HttpEntity<Void>(bearerHeaders(token)),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---- ownership: another user's trip is indistinguishable from missing (404) ----

    @Test
    fun `get another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val id = createTrip(tokenA)?.get("id")!!

        val response = get(tokenB, id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `update another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val id = createTrip(tokenA)?.get("id")!!

        val entity = HttpEntity(
            mapOf("title" to "Hijacked"),
            bearerHeaders(tokenB),
        )
        val response = rest.exchange("/api/trips/$id", HttpMethod.PUT, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete another users trip returns 404`() {
        val tokenA = rest.registerAndGetToken()
        val tokenB = rest.registerAndGetToken()
        val id = createTrip(tokenA)?.get("id")!!

        val response = rest.exchange<Map<*, *>>(
            "/api/trips/$id",
            HttpMethod.DELETE,
            HttpEntity<Void>(bearerHeaders(tokenB)),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---- destination anchor: clearing on full-replace update ----

    @Test
    fun `update without a destination clears the anchor`() {
        val token = rest.registerAndGetToken()
        val id = createTrip(token)?.get("id")!!

        // PUT is a full replace: omitting the destination clears it.
        val entity = HttpEntity(
            mapOf(
                "title" to "Japan 2026",
                "startDate" to "2026-05-01",
                "endDate" to "2026-05-10",
                "version" to 0,
            ),
            bearerHeaders(token),
        )
        val response = rest.exchange("/api/trips/$id", HttpMethod.PUT, entity, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("destination")).isNull()
    }

    // ---- optimistic locking (409 / 400) ----

    @Test
    fun `updating with a stale version returns 409`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token)["id"]!!

        // first update: version 0 → succeeds, trip becomes version 1
        val first = rest.exchange(
            "/api/trips/$tripId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "First", "startDate" to "2026-05-01", "endDate" to "2026-05-10", "version" to 0),
                bearerHeaders(token),
            ),
            Map::class.java,
        )
        assertThat(first.statusCode).isEqualTo(HttpStatus.OK)

        // second update reusing stale version 0 → conflict
        val stale = rest.exchange<String>(
            "/api/trips/$tripId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "Stale", "startDate" to "2026-05-01", "endDate" to "2026-05-10", "version" to 0),
                bearerHeaders(token),
            ),
        )
        assertThat(stale.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `a fresh version allows a subsequent update`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token).get("id")!!

        // update with version 0 → response should carry version 1
        val first = rest.exchange(
            "/api/trips/$tripId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "First", "startDate" to "2026-05-01", "endDate" to "2026-05-10", "version" to 0),
                bearerHeaders(token),
            ),
            Map::class.java,
        )
        assertThat(first.statusCode).isEqualTo(HttpStatus.OK)
        val newVersion = (first.body?.get("version") as Number).toInt()
        assertThat(newVersion).isEqualTo(1)

        // update again with the CURRENT version → succeeds
        val second = rest.exchange(
            "/api/trips/$tripId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "Second", "startDate" to "2026-05-01", "endDate" to "2026-05-10", "version" to newVersion),
                bearerHeaders(token),
            ),
            Map::class.java,
        )
        assertThat(second.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `updating without a version returns 400`() {
        val token = rest.registerAndGetToken()
        val tripId = rest.createTrip(token).get("id")!!

        val response = rest.exchange<String>(
            "/api/trips/$tripId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "No version", "startDate" to "2026-05-01", "endDate" to "2026-05-10"),
                bearerHeaders(token),
            ),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}