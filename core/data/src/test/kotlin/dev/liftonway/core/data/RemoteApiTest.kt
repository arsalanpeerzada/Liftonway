package dev.liftonway.core.data

import dev.liftonway.core.data.remote.LiftApi
import dev.liftonway.core.model.NotificationKind
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.RideQuery
import dev.liftonway.core.model.TripStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** The app's side of the API contract: JSON in, models out, and problems turned into messages. */
class RemoteApiTest {
    private class FakeTokens(var value: String? = "test-token", var refresh: String? = "refresh-1") : TokenSource {
        var expired = false
        override suspend fun token() = value
        override suspend fun refreshToken() = refresh
        override suspend fun update(access: String, refresh: String) {
            value = access
            this.refresh = refresh
        }
        override suspend fun expire() {
            expired = true
            value = null
            refresh = null
        }
    }

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val f7 = Place("f7", "F-7 Markaz", "Islamabad")
    private val saddar = Place("saddar", "Saddar", "Rawalpindi")
    private val query = RideQuery(f7, saddar, LocalDate.of(2026, 10, 20), seats = 1)

    private fun rides(tokens: TokenSource = FakeTokens(), handler: MockRequestHandler) =
        RemoteRideRepository(LiftApi(tokens, MockEngine(handler)), Clock.systemUTC())

    private val rideJson = """
        {"id":"r1","driver":{"id":"d1","name":"Hamza Tariq","rating":4.8},
         "departure":"2026-10-20T03:00:00Z",
         "points":[{"id":"f7","title":"F-7 Markaz","area":"Islamabad"},{"id":"faizabad","title":"Faizabad Interchange","area":"Islamabad"},
                   {"id":"saddar","title":"Saddar","area":"Rawalpindi"}],
         "vehicleName":"Honda Civic","seatsTotal":3,"seatsLeft":2,"pricePerSeat":450,"route":"","extraFieldFromANewerServer":true}
    """.trimIndent()

    @Test
    fun `search sends the query and maps rides, in Pakistan time`() = runTest {
        var url = ""
        var auth: String? = null
        val found = rides { request ->
            url = request.url.toString()
            auth = request.headers[HttpHeaders.Authorization]
            respond("[$rideJson]", HttpStatusCode.OK, json)
        }.search(query)

        assertTrue(url.endsWith("rides?from=f7&to=saddar&date=2026-10-20&seats=1"))
        assertEquals("Bearer test-token", auth)
        val ride = found.single()
        assertEquals(LocalDateTime.of(2026, 10, 20, 8, 0), ride.departure) // 03:00 UTC is 08:00 in Pakistan.
        assertEquals("f7", ride.pickup.id)
        assertEquals(listOf("faizabad"), ride.stops.map { it.id })
        assertEquals("saddar", ride.destination.id)
        assertEquals(450L, ride.pricePerSeat)
        assertEquals(4.8f, ride.driver.rating)
    }

    @Test
    fun `problem responses become the server's message`() = runTest {
        val repo = rides { _ ->
            respond(
                """{"status":409,"code":"NO_SEATS","detail":"Not enough seats left on this ride."}""",
                HttpStatusCode.Conflict,
                headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        }
        try {
            repo.search(query)
            fail("Expected ApiError")
        } catch (e: ApiError) {
            assertEquals("NO_SEATS", e.code)
            assertEquals("Not enough seats left on this ride.", e.message)
        }
    }

    @Test
    fun `a closed cancel window is reported as too late, not as an error`() = runTest {
        val result = rides { _ ->
            respond(
                """{"code":"CANCEL_WINDOW_CLOSED","detail":"Ride can only be canceled before 4 hours."}""",
                HttpStatusCode.Conflict,
                headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        }.cancel("b1")
        assertEquals(CancelResult.TooLate, result)
    }

    @Test
    fun `an ended session signs the user out`() = runTest {
        val tokens = FakeTokens()
        try {
            rides(tokens) { _ -> respond("", HttpStatusCode.Unauthorized) }.refreshBookings()
            fail("Expected ApiError")
        } catch (e: ApiError) {
            assertEquals(LiftApi.SIGNED_OUT, e.code)
            assertTrue(tokens.expired)
            assertNull(tokens.value)
        }
    }

    @Test
    fun `an expired access token is renewed once and the call repeated, even when calls fail together`() = runTest {
        val tokens = FakeTokens(value = "old-access")
        var refreshes = 0
        val repository = rides(tokens) { request ->
            when {
                request.url.encodedPath.endsWith("auth/refresh") -> {
                    refreshes++
                    respond(
                        """{"token":"new-access","expiresAt":"2026-10-19T05:15:00Z","refreshToken":"refresh-2","refreshExpiresAt":"2026-12-18T05:00:00Z"}""",
                        HttpStatusCode.OK,
                        json,
                    )
                }
                request.headers[HttpHeaders.Authorization] == "Bearer new-access" -> respond("[]", HttpStatusCode.OK, json)
                else -> respond("", HttpStatusCode.Unauthorized)
            }
        }
        coroutineScope {
            launch { repository.refreshBookings() }
            launch { repository.refreshPublished() }
        }
        assertEquals(1, refreshes)
        assertEquals("new-access", tokens.value)
        assertEquals("refresh-2", tokens.refresh)
        assertFalse(tokens.expired)
    }

    @Test
    fun `a renewal that fails because the server is down keeps the user signed in`() = runTest {
        val tokens = FakeTokens()
        try {
            rides(tokens) { request ->
                if (request.url.encodedPath.endsWith("auth/refresh")) respond("", HttpStatusCode.ServiceUnavailable) else respond("", HttpStatusCode.Unauthorized)
            }.refreshBookings()
            fail("Expected ApiError")
        } catch (e: ApiError) {
            assertFalse(tokens.expired)
            assertEquals("refresh-1", tokens.refresh)
        }
    }

    @Test
    fun `no connection gives a friendly offline message`() = runTest {
        try {
            rides { _ -> throw IOException("connection refused") }.search(query)
            fail("Expected ApiError")
        } catch (e: ApiError) {
            assertEquals(ApiError.OFFLINE, e.code)
            assertFalse(e.message.contains("refused"))
        }
    }

    @Test
    fun `bookings map their trip status and confirmation`() = runTest {
        val repo = rides { _ ->
            respond(
                """[{"id":"b1","ride":$rideJson,"seats":1,"status":"CONFIRMED","tripStatus":"UPCOMING","canCancel":true},
                    {"id":"b2","ride":$rideJson,"seats":2,"status":"DECLINED","tripStatus":"CANCELLED","canCancel":false}]""",
                HttpStatusCode.OK,
                json,
            )
        }
        repo.refreshBookings()
        val (confirmed, declined) = repo.bookings.value
        assertTrue(confirmed.confirmed)
        assertEquals(TripStatus.Upcoming, confirmed.status)
        assertEquals(TripStatus.Cancelled, declined.status)
    }

    @Test
    fun `notifications map to messages, and answering a request goes through its booking`() = runTest {
        val calls = mutableListOf<String>()
        val api = LiftApi(FakeTokens(), MockEngine { request ->
            calls += "${request.method.value} ${request.url.encodedPath}"
            when {
                request.url.encodedPath.endsWith("me/notifications") -> respond(
                    """[{"id":"n1","kind":"RIDE_REQUEST","actor":{"id":"u2","name":"Asma Rider"},
                        "ride":{"id":"r1","from":"Blue Area, Jinnah Avenue","to":"Islamabad International Airport","departure":"2026-10-20T03:00:00Z"},
                        "bookingId":"b1","seats":2,"bookingStatus":"PENDING","actionable":true,"read":false,"createdAt":"2026-10-19T05:00:00Z"},
                       {"id":"n2","kind":"RIDE_REQUEST","actor":{"id":"u3","name":"Omar Rider"},"bookingId":"b2","seats":1,
                        "bookingStatus":"CANCELLED","actionable":false,"read":true,"createdAt":"2026-10-19T04:00:00Z"},
                       {"id":"n3","kind":"SOMETHING_NEW","createdAt":"2026-10-19T03:00:00Z"}]""",
                    HttpStatusCode.OK,
                    json,
                )
                request.url.encodedPath.endsWith("me/rides") -> respond("[]", HttpStatusCode.OK, json)
                else -> respond("""{"id":"b1"}""", HttpStatusCode.OK, json)
            }
        })
        val notifications = RemoteNotificationRepository(api, RemoteRideRepository(api, Clock.systemUTC()), Clock.systemUTC())
        notifications.refresh()

        // Kinds from a newer server are skipped rather than shown wrongly.
        val (request, withdrawn) = notifications.items.value
        assertEquals(2, notifications.items.value.size)
        assertEquals(NotificationKind.RideRequest, request.kind)
        assertEquals("wants 2 seats on your ride from Blue Area to Islamabad International Airport", request.message)
        assertTrue(request.actionable)
        assertEquals("Withdrawn", withdrawn.outcome)

        notifications.answer("n1", accept = true)
        assertTrue(calls.toString(), "POST /api/v1/bookings/b1/accept" in calls)
    }

    @Test
    fun `a published ride carries its passengers and what the driver may do`() = runTest {
        val repository = rides { request ->
            when (request.url.encodedPath.substringAfter("/api/v1/")) {
                "me/rides" -> respond(
                    """[{"ride":$rideJson,"tripStatus":"UPCOMING","canCancel":false,"canComplete":false,
                         "passengers":[{"bookingId":"b1","passengerId":"u2","name":"Asma Rider","seats":1,"status":"CONFIRMED"},
                                       {"bookingId":"b2","passengerId":"u3","name":"Omar Rider","seats":2,"status":"PENDING"}]}]""",
                    HttpStatusCode.OK,
                    json,
                )
                else -> respond("""{"status":409,"code":"CANCEL_WINDOW_CLOSED","detail":"Too late."}""", HttpStatusCode.Conflict, json)
            }
        }
        repository.refreshPublished()
        val ride = repository.published.value.single()
        assertEquals(listOf("Omar Rider"), ride.waiting.map { it.person.name })
        assertEquals(1, ride.seatsBooked)
        assertEquals(CancelResult.TooLate, repository.cancelRide("r1"))
    }
}
