package dev.liftonway.feature.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.CancelResult
import dev.liftonway.core.data.RideRepository
import dev.liftonway.core.model.Booking
import dev.liftonway.core.model.PublishedRide
import dev.liftonway.core.model.TripStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(private val rides: RideRepository) : ViewModel() {
    val bookings: StateFlow<Map<TripStatus, List<Booking>>> = rides.bookings
        .map { list -> list.sortedBy { it.ride.departure }.groupBy { it.status } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), rides.bookings.value.sortedBy { it.ride.departure }.groupBy { it.status })

    val published: StateFlow<Map<TripStatus, List<PublishedRide>>> = rides.published
        .map { list -> list.sortedBy { it.ride.departure }.groupBy { it.status } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), rides.published.value.sortedBy { it.ride.departure }.groupBy { it.status })

    /** The booking whose cancel sheet is open. */
    var cancelling by mutableStateOf<Booking?>(null)
        private set
    var cancelBusy by mutableStateOf(false)
        private set

    fun canCancel(booking: Booking) = rides.canCancel(booking)

    fun askCancel(booking: Booking) {
        cancelling = booking
    }

    fun dismissCancel() {
        if (!cancelBusy) cancelling = null
    }

    fun confirmCancel(onDone: () -> Unit) {
        val booking = cancelling ?: return
        viewModelScope.launch {
            cancelBusy = true
            val result = rides.cancel(booking.id)
            cancelBusy = false
            if (result == CancelResult.Cancelled) onDone()
        }
    }
}
