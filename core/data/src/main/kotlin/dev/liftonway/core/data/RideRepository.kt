package dev.liftonway.core.data

import dev.liftonway.core.model.Booking
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.PublishedRide
import dev.liftonway.core.model.Ride
import dev.liftonway.core.model.RideDraft
import dev.liftonway.core.model.RideQuery
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

sealed interface CancelResult {
    data object Cancelled : CancelResult
    data object TooLate : CancelResult
}

/**
 * Rides to find, the user's seat requests and the rides they publish. Calls throw [ApiError] when they fail.
 * [bookings] and [published] hold the last loaded lists; the refresh functions reload them.
 */
interface RideRepository {
    val bookings: StateFlow<List<Booking>>
    val published: StateFlow<List<PublishedRide>>

    fun now(): LocalDateTime

    suspend fun places(query: String): List<Place>
    suspend fun search(query: RideQuery): List<Ride>

    /** The same route on other days, for the "No rides available" page. */
    suspend fun recommended(query: RideQuery): List<Ride>
    suspend fun ride(id: String): Ride?
    suspend fun request(ride: Ride, seats: Int): Booking
    suspend fun cancel(bookingId: String): CancelResult
    suspend fun publish(draft: RideDraft, pricePerSeat: Long): PublishedRide
    suspend fun refreshBookings()
    suspend fun refreshPublished()
}
