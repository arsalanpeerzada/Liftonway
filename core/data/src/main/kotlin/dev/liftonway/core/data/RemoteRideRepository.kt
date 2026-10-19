package dev.liftonway.core.data

import dev.liftonway.core.data.remote.BookingDto
import dev.liftonway.core.data.remote.LiftApi
import dev.liftonway.core.data.remote.PakistanTime
import dev.liftonway.core.data.remote.PlaceDto
import dev.liftonway.core.data.remote.PublishBody
import dev.liftonway.core.data.remote.PublishedRideDto
import dev.liftonway.core.data.remote.RideDto
import dev.liftonway.core.data.remote.SeatsBody
import dev.liftonway.core.data.remote.toApiTime
import dev.liftonway.core.data.remote.toModel
import dev.liftonway.core.model.Booking
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.PublishedRide
import dev.liftonway.core.model.Ride
import dev.liftonway.core.model.RideDraft
import dev.liftonway.core.model.RideQuery
import dev.liftonway.core.model.TripStatus
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Rides through the Lift On Way API. The server applies the 4-hour rule; the app mirrors it only for display. */
@Singleton
class RemoteRideRepository @Inject constructor(private val api: LiftApi, private val clock: Clock) : RideRepository {
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    private val _published = MutableStateFlow<List<PublishedRide>>(emptyList())
    override val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()
    override val published: StateFlow<List<PublishedRide>> = _published.asStateFlow()

    override fun now(): LocalDateTime = LocalDateTime.now(clock.withZone(PakistanTime))

    override suspend fun places(query: String): List<Place> = api.call<List<PlaceDto>> {
        url("places")
        parameter("q", query.trim())
    }.map { it.toModel() }

    override suspend fun search(query: RideQuery): List<Ride> = api.call<List<RideDto>> {
        url("rides")
        parameter("from", query.pickup.id)
        parameter("to", query.destination.id)
        parameter("date", query.date.toString())
        parameter("seats", query.seats)
    }.map { it.toModel() }

    override suspend fun recommended(query: RideQuery): List<Ride> = api.call<List<RideDto>> {
        url("rides/recommended")
        parameter("from", query.pickup.id)
        parameter("to", query.destination.id)
        parameter("excluding", query.date.toString())
        parameter("seats", query.seats)
    }.map { it.toModel() }

    override suspend fun ride(id: String): Ride? = try {
        api.call<RideDto> { url("rides/$id") }.toModel()
    } catch (e: ApiError) {
        if (e.code == "NOT_FOUND") null else throw e
    }

    override suspend fun request(ride: Ride, seats: Int): Booking {
        val booking = api.call<BookingDto> {
            method = HttpMethod.Post
            url("rides/${ride.id}/bookings")
            setBody(SeatsBody(seats))
        }.toModel()
        refreshBookings()
        return booking
    }

    override suspend fun cancel(bookingId: String): CancelResult {
        try {
            api.exec {
                method = HttpMethod.Post
                url("bookings/$bookingId/cancel")
            }
        } catch (e: ApiError) {
            if (e.code == "CANCEL_WINDOW_CLOSED") return CancelResult.TooLate
            throw e
        }
        refreshBookings()
        return CancelResult.Cancelled
    }

    override suspend fun publish(draft: RideDraft, pricePerSeat: Long): PublishedRide {
        val ride = api.call<RideDto> {
            method = HttpMethod.Post
            url("rides")
            setBody(
                PublishBody(
                    from = requireNotNull(draft.pickup).id,
                    to = requireNotNull(draft.destination).id,
                    stops = draft.stops.map { it.id },
                    departure = LocalDateTime.of(draft.date, draft.time).toApiTime(),
                    seats = draft.seats,
                    pricePerSeat = pricePerSeat.toInt(),
                    route = draft.route,
                ),
            )
        }.toModel()
        refreshPublished()
        return _published.value.firstOrNull { it.ride.id == ride.id } ?: PublishedRide(ride, TripStatus.Upcoming, emptyList())
    }

    override suspend fun answer(bookingId: String, accept: Boolean) {
        api.exec {
            method = HttpMethod.Post
            url("bookings/$bookingId/${if (accept) "accept" else "decline"}")
        }
        refreshPublished()
    }

    override suspend fun cancelRide(rideId: String): CancelResult {
        try {
            api.exec {
                method = HttpMethod.Post
                url("rides/$rideId/cancel")
            }
        } catch (e: ApiError) {
            if (e.code == "CANCEL_WINDOW_CLOSED") return CancelResult.TooLate
            throw e
        }
        refreshPublished()
        return CancelResult.Cancelled
    }

    override suspend fun completeRide(rideId: String) {
        api.exec {
            method = HttpMethod.Post
            url("rides/$rideId/complete")
        }
        refreshPublished()
    }

    override suspend fun refreshBookings() {
        _bookings.value = api.call<List<BookingDto>> { url("me/bookings") }.map { it.toModel() }
    }

    override suspend fun refreshPublished() {
        _published.value = api.call<List<PublishedRideDto>> { url("me/rides") }.map { it.toModel() }
    }
}
