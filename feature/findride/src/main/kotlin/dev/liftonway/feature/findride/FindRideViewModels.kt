package dev.liftonway.feature.findride

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.RideRepository
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.Ride
import dev.liftonway.core.model.RideQuery
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** What Available rides needs to repeat a search; plain values so it fits in a navigation key. */
data class RideSearch(val pickupId: String, val destinationId: String, val epochDay: Long, val seats: Int)

enum class LocationField { Pickup, Destination }

@HiltViewModel
class FindRideViewModel @Inject constructor(private val rides: RideRepository) : ViewModel() {
    var pickup by mutableStateOf<Place?>(null)
        private set
    var destination by mutableStateOf<Place?>(null)
        private set
    var pickupText by mutableStateOf("")
        private set
    var destinationText by mutableStateOf("")
        private set
    var date: LocalDate by mutableStateOf(LocalDate.now())
    var seats by mutableStateOf(1)
        private set
    var showErrors by mutableStateOf(false)
        private set

    fun text(field: LocationField) = if (field == LocationField.Pickup) pickupText else destinationText

    fun onText(field: LocationField, value: String) {
        if (field == LocationField.Pickup) {
            pickupText = value
            pickup = null
        } else {
            destinationText = value
            destination = null
        }
    }

    fun suggestions(field: LocationField): List<Place> {
        val other = if (field == LocationField.Pickup) destination else pickup
        return rides.places(text(field)).filter { it.id != other?.id }.take(5)
    }

    fun choose(field: LocationField, place: Place) {
        if (field == LocationField.Pickup) {
            pickup = place
            pickupText = place.label
        } else {
            destination = place
            destinationText = place.label
        }
    }

    fun changeSeats(delta: Int) {
        seats = (seats + delta).coerceIn(1, Policy.MAX_SEATS)
    }

    val pickupError get() = showErrors && pickup == null
    val destinationError get() = showErrors && destination == null

    fun search(onSearch: (RideSearch) -> Unit) {
        showErrors = true
        val from = pickup ?: return
        val to = destination ?: return
        onSearch(RideSearch(from.id, to.id, date.toEpochDay(), seats))
    }
}

sealed interface RidesState {
    data object Searching : RidesState
    data class Found(val rides: List<Ride>) : RidesState
    data class None(val recommended: List<Ride>) : RidesState
}

enum class SortOrder(val label: String) { Earliest("Earliest departure"), Cheapest("Lowest price"), Rating("Highest rated") }

enum class RequestState { Idle, Sending, Sent }

@HiltViewModel(assistedFactory = AvailableRidesViewModel.Factory::class)
class AvailableRidesViewModel @AssistedInject constructor(
    @Assisted val search: RideSearch,
    private val rides: RideRepository,
) : ViewModel() {
    private val query = RideQuery(rides.place(search.pickupId), rides.place(search.destinationId), LocalDate.ofEpochDay(search.epochDay), search.seats)

    var state by mutableStateOf<RidesState>(RidesState.Searching)
        private set
    var sort by mutableStateOf(SortOrder.Earliest)
    val requests = mutableStateMapOf<String, RequestState>()

    init {
        viewModelScope.launch {
            val found = rides.search(query)
            state = if (found.isEmpty()) RidesState.None(rides.recommended(query)) else RidesState.Found(found)
        }
    }

    fun sorted(list: List<Ride>): List<Ride> = when (sort) {
        SortOrder.Earliest -> list.sortedBy { it.departure }
        SortOrder.Cheapest -> list.sortedBy { it.pricePerSeat }
        SortOrder.Rating -> list.sortedByDescending { it.driver.rating }
    }

    fun request(ride: Ride, onSent: (Ride) -> Unit) {
        if (requests[ride.id] != null) return
        requests[ride.id] = RequestState.Sending
        viewModelScope.launch {
            rides.request(ride, search.seats)
            requests[ride.id] = RequestState.Sent
            onSent(ride)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(search: RideSearch): AvailableRidesViewModel
    }
}

@HiltViewModel(assistedFactory = RideStopsViewModel.Factory::class)
class RideStopsViewModel @AssistedInject constructor(@Assisted rideId: String, rides: RideRepository) : ViewModel() {
    val ride: Ride? = rides.ride(rideId)

    @AssistedFactory
    interface Factory {
        fun create(rideId: String): RideStopsViewModel
    }
}
