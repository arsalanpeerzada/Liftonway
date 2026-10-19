package dev.liftonway.core.data

import dev.liftonway.core.model.Booking
import dev.liftonway.core.model.Passenger
import dev.liftonway.core.model.Person
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.PublishedRide
import dev.liftonway.core.model.Ride
import dev.liftonway.core.model.RideDraft
import dev.liftonway.core.model.RideQuery
import dev.liftonway.core.model.TripStatus
import dev.liftonway.core.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * The offline demo (`-Pliftonway.fake=true`): rides, requests and published rides all in memory.
 * Search results are generated from the query, so the same search always gives the same rides.
 */
@Singleton
class FakeRideRepository @Inject constructor(
    private val clock: Clock,
    @param:AppScope private val scope: CoroutineScope,
    private val vehicles: VehicleRepository,
    private val me: CurrentPerson,
) : RideRepository {
    private val cache = mutableMapOf<String, Ride>()
    private val _bookings = MutableStateFlow(seedBookings())
    private val _published = MutableStateFlow<List<PublishedRide>>(emptyList())

    override val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()
    override val published: StateFlow<List<PublishedRide>> = _published.asStateFlow()

    override fun now(): LocalDateTime = LocalDateTime.now(clock)

    override suspend fun places(query: String): List<Place> {
        val q = query.trim()
        if (q.isEmpty()) return SampleData.places.take(6)
        return SampleData.places.filter { it.label.contains(q, ignoreCase = true) }
    }

    fun place(id: String): Place = SampleData.place(id)

    override suspend fun ride(id: String): Ride? = cache[id]
        ?: _bookings.value.firstOrNull { it.ride.id == id }?.ride
        ?: _published.value.firstOrNull { it.ride.id == id }?.ride

    /** Rides run on the next seven days; further out there's nothing yet, which shows the recommendations. */
    override suspend fun search(query: RideQuery): List<Ride> {
        delay(1400) // Long enough for the searching animation to read as work, short enough not to annoy.
        val days = ChronoUnit.DAYS.between(LocalDate.now(clock), query.date)
        if (days !in 0..SEARCH_WINDOW_DAYS || query.pickup.id == query.destination.id) return emptyList()
        return generate(query.pickup, query.destination, query.date, query.seats)
    }

    /** The soonest rides on the same route on other days this week, one per driver, for the "No rides available" page. */
    override suspend fun recommended(query: RideQuery): List<Ride> {
        val today = LocalDate.now(clock)
        return (0L..SEARCH_WINDOW_DAYS)
            .flatMap { generate(query.pickup, query.destination, today.plusDays(it), query.seats) }
            .filter { it.departure.toLocalDate() != query.date }
            .distinctBy { it.driver.id }
            .take(3)
    }

    override suspend fun request(ride: Ride, seats: Int): Booking {
        delay(1100)
        val booking = Booking(UUID.randomUUID().toString(), ride, seats, TripStatus.Upcoming, confirmed = false)
        _bookings.update { listOf(booking) + it }
        // A driver answers a few seconds later, so the Pending badge visibly turns into Confirmed.
        scope.launch {
            delay(6000)
            _bookings.update { list -> list.map { if (it.id == booking.id && it.status == TripStatus.Upcoming) it.copy(confirmed = true) else it } }
        }
        return booking
    }

    fun canCancel(booking: Booking): Boolean = Policy.canCancel(booking.ride.departure, now())

    override suspend fun cancel(bookingId: String): CancelResult {
        val booking = _bookings.value.first { it.id == bookingId }
        if (!canCancel(booking)) return CancelResult.TooLate
        delay(700)
        _bookings.update { list -> list.map { if (it.id == bookingId) it.copy(status = TripStatus.Cancelled) else it } }
        return CancelResult.Cancelled
    }

    override suspend fun publish(draft: RideDraft, pricePerSeat: Long): PublishedRide {
        val vehicle = vehicles.vehicle.value ?: throw ApiError("VEHICLE_REQUIRED", "Add your vehicle before publishing a ride.")
        val driver = me.get()
        delay(1300)
        val ride = Ride(
            id = UUID.randomUUID().toString(),
            driver = driver,
            departure = LocalDateTime.of(draft.date, draft.time),
            pickup = requireNotNull(draft.pickup),
            destination = requireNotNull(draft.destination),
            stops = draft.stops,
            vehicleName = vehicle.name,
            totalSeats = draft.seats,
            pricePerSeat = pricePerSeat,
            route = draft.route,
        )
        val published = PublishedRide(ride, TripStatus.Upcoming, passengers = emptyList(), canCancel = true)
        _published.update { listOf(published) + it }
        // Someone asks for a seat shortly after, so the driver side has something to answer.
        scope.launch {
            delay(5000)
            val rider = SampleData.drivers.last()
            updatePublished(ride.id) { it.copy(passengers = it.passengers + Passenger("req-${ride.id}", rider, seats = 1, confirmed = false)) }
        }
        return published
    }

    override suspend fun answer(bookingId: String, accept: Boolean) {
        delay(600)
        _published.update { list ->
            list.map { p ->
                p.copy(passengers = p.passengers.mapNotNull { if (it.bookingId != bookingId) it else if (accept) it.copy(confirmed = true) else null })
            }
        }
    }

    override suspend fun cancelRide(rideId: String): CancelResult {
        val ride = _published.value.first { it.ride.id == rideId }
        if (ride.passengers.any { it.confirmed } && !Policy.canCancel(ride.ride.departure, now())) return CancelResult.TooLate
        delay(700)
        updatePublished(rideId) { it.copy(status = TripStatus.Cancelled, canCancel = false, canComplete = false) }
        return CancelResult.Cancelled
    }

    override suspend fun completeRide(rideId: String) {
        delay(700)
        updatePublished(rideId) { it.copy(status = TripStatus.Completed, passengers = it.passengers.filter { p -> p.confirmed }, canCancel = false, canComplete = false) }
    }

    private fun updatePublished(rideId: String, change: (PublishedRide) -> PublishedRide) {
        _published.update { list -> list.map { if (it.ride.id == rideId) change(it) else it } }
    }

    override suspend fun refreshBookings() = Unit

    /** Rides that have set off can be completed; the cancel rule is re-checked against the clock. */
    override suspend fun refreshPublished() {
        val now = now()
        _published.update { list ->
            list.map {
                if (it.status != TripStatus.Upcoming) return@map it
                val departed = !it.ride.departure.isAfter(now)
                it.copy(
                    canComplete = departed,
                    canCancel = !departed && (it.passengers.none { p -> p.confirmed } || Policy.canCancel(it.ride.departure, now)),
                )
            }
        }
    }

    private fun generate(pickup: Place, destination: Place, date: LocalDate, seats: Int): List<Ride> {
        val seed = (pickup.id + destination.id + date).hashCode().absoluteValue
        val now = now()
        val times = listOf(LocalTime.of(7, 0), LocalTime.of(8, 30), LocalTime.of(12, 15), LocalTime.of(17, 45), LocalTime.of(19, 30))
        // Stops stay in the cities the ride runs between.
        val stopCandidates = SampleData.places.filter { it.id != pickup.id && it.id != destination.id && it.area in setOf(pickup.area, destination.area) }
        return times.mapIndexedNotNull { i, time ->
            val departure = LocalDateTime.of(date, time)
            if (departure.isBefore(now.plusMinutes(30)) || (seed + i) % 4 == 3) return@mapIndexedNotNull null
            val driver = SampleData.drivers[(seed + i) % SampleData.drivers.size]
            val totalSeats = 2 + (seed + i * 3) % 3
            if (totalSeats < seats) return@mapIndexedNotNull null
            val id = "r-$seed-$i"
            cache.getOrPut(id) {
                Ride(
                    id = id,
                    driver = driver,
                    departure = departure,
                    pickup = pickup,
                    destination = destination,
                    stops = if ((seed + i) % 2 == 0 && stopCandidates.isNotEmpty()) listOf(stopCandidates[(seed + i) % stopCandidates.size]) else emptyList(),
                    vehicleName = SampleData.vehicles[(seed + i * 7) % SampleData.vehicles.size],
                    totalSeats = totalSeats,
                    pricePerSeat = 350L + ((seed / 7 + i * 113) % 12) * 50L,
                )
            }
        }
    }

    private fun seedBookings(): List<Booking> {
        val today = LocalDate.now(clock)
        val soon = LocalDateTime.now(clock).plusHours(2).truncatedTo(ChronoUnit.HOURS)
        fun ride(id: String, driver: Person, departure: LocalDateTime, from: String, to: String, car: String, price: Long) = Ride(
            id = id, driver = driver, departure = departure, pickup = SampleData.place(from), destination = SampleData.place(to),
            stops = emptyList(), vehicleName = car, totalSeats = 3, pricePerSeat = price,
        ).also { cache[id] = it }
        return listOf(
            Booking("b1", ride("seed-1", SampleData.drivers[0], LocalDateTime.of(today.plusDays(1), LocalTime.of(8, 0)), "f7", "saddar", "Toyota Corolla", 450), 1, TripStatus.Upcoming, confirmed = false),
            Booking("b2", ride("seed-2", SampleData.drivers[1], soon, "blue", "bahria", "Honda Civic", 600), 1, TripStatus.Upcoming, confirmed = true),
            Booking("b3", ride("seed-3", SampleData.drivers[2], LocalDateTime.of(today.minusDays(3), LocalTime.of(17, 45)), "g11", "airport", "Suzuki Cultus", 800), 2, TripStatus.Completed, confirmed = true),
        )
    }

    companion object {
        /** Rides are published up to a week ahead; searches further out find nothing. */
        const val SEARCH_WINDOW_DAYS = 6L
    }
}
