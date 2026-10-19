package dev.liftonway.feature.publish

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.RideRepository
import dev.liftonway.core.data.VehicleRepository
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.RideDraft
import dev.liftonway.core.model.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/** One editable address line: what's typed, and the place once one is chosen. */
class AddressField(val id: String = UUID.randomUUID().toString()) {
    var text by mutableStateOf("")
    var place by mutableStateOf<Place?>(null)
    var suggestions by mutableStateOf<List<Place>>(emptyList())
}

/** The publish form. The price per seat wasn't in the designs; it follows the seats picker's style. */
@HiltViewModel
class PublishViewModel @Inject constructor(
    private val rides: RideRepository,
    vehicles: VehicleRepository,
) : ViewModel() {
    val pickup = AddressField()
    val destination = AddressField()
    val stops = mutableStateListOf<AddressField>()
    var date: LocalDate by mutableStateOf(LocalDate.now().plusDays(1))
    var time: LocalTime by mutableStateOf(LocalTime.of(8, 0))
    var seats by mutableStateOf(1)
        private set
    var price by mutableStateOf(Policy.DEFAULT_FARE)
    var route by mutableStateOf("")
    var showErrors by mutableStateOf(false)
        private set
    var publishing by mutableStateOf(false)
        private set
    var published by mutableStateOf(false)
        private set

    /** Why the last publish attempt failed, from the server or the connection. */
    var failure by mutableStateOf<String?>(null)
        private set
    private var lookup: Job? = null

    val vehicle: StateFlow<Vehicle?> = vehicles.vehicle.stateIn(viewModelScope, SharingStarted.Eagerly, vehicles.vehicle.value)

    init {
        viewModelScope.launch { runCatching { vehicles.refresh() } }
    }

    /** Loads places for [field], a moment after typing stops; a failed lookup just shows nothing. */
    fun lookUp(field: AddressField) {
        lookup?.cancel()
        lookup = viewModelScope.launch {
            delay(150)
            val taken = (listOf(pickup, destination) + stops).filter { it !== field }.mapNotNull { it.place?.id }.toSet()
            field.suggestions = runCatching { rides.places(field.text) }.getOrDefault(emptyList()).filterNot { it.id in taken }.take(5)
        }
    }

    fun onText(field: AddressField, value: String) {
        field.text = value
        field.place = null
        lookUp(field)
    }

    fun choose(field: AddressField, place: Place) {
        field.place = place
        field.text = place.label
    }

    fun addStop() {
        if (stops.size < MAX_STOPS) stops.add(AddressField())
    }

    fun removeStop(field: AddressField) {
        stops.remove(field)
    }

    fun changeSeats(delta: Int) {
        seats = (seats + delta).coerceIn(1, Policy.MAX_SEATS)
    }

    val missingVehicle get() = showErrors && vehicle.value == null
    fun error(field: AddressField) = showErrors && field.place == null

    fun publish() {
        showErrors = true
        val car = vehicle.value
        if (pickup.place == null || destination.place == null || car == null || stops.any { it.place == null } || publishing) return
        viewModelScope.launch {
            publishing = true
            failure = null
            val draft = RideDraft(pickup.place, destination.place, stops.mapNotNull { it.place }, date, time, seats, route.trim())
            try {
                rides.publish(draft, price)
                published = true
            } catch (e: ApiError) {
                failure = e.message
            } finally {
                publishing = false
            }
        }
    }

    companion object {
        const val MAX_STOPS = 3
    }
}
