package dev.liftonway.feature.publish

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.RideRepository
import dev.liftonway.core.data.SessionRepository
import dev.liftonway.core.data.VehicleRepository
import dev.liftonway.core.model.Person
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.RideDraft
import dev.liftonway.core.model.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/** One editable address line: what's typed, and the place once one is chosen. */
class AddressField(val id: String = UUID.randomUUID().toString()) {
    var text by mutableStateOf("")
    var place by mutableStateOf<Place?>(null)
}

/**
 * The publish form. The designs have no price field, so the prototype asks a flat suggested fare per seat;
 * a price input is a gap to raise with design.
 */
@HiltViewModel
class PublishViewModel @Inject constructor(
    private val rides: RideRepository,
    private val session: SessionRepository,
    vehicles: VehicleRepository,
) : ViewModel() {
    val pickup = AddressField()
    val destination = AddressField()
    val stops = mutableStateListOf<AddressField>()
    var date: LocalDate by mutableStateOf(LocalDate.now().plusDays(1))
    var time: LocalTime by mutableStateOf(LocalTime.of(8, 0))
    var seats by mutableStateOf(1)
        private set
    var route by mutableStateOf("")
    var showErrors by mutableStateOf(false)
        private set
    var publishing by mutableStateOf(false)
        private set
    var published by mutableStateOf(false)
        private set

    val vehicle: StateFlow<Vehicle?> = vehicles.vehicle.stateIn(viewModelScope, SharingStarted.Eagerly, vehicles.vehicle.value)

    fun suggestions(field: AddressField): List<Place> {
        val taken = (listOf(pickup, destination) + stops).filter { it !== field }.mapNotNull { it.place?.id }.toSet()
        return rides.places(field.text).filterNot { it.id in taken }.take(5)
    }

    fun onText(field: AddressField, value: String) {
        field.text = value
        field.place = null
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
            val profile = session.session.first().profile
            val me = Person("me", profile?.name ?: "You", 5f, profile?.photoUri)
            val draft = RideDraft(pickup.place, destination.place, stops.mapNotNull { it.place }, date, time, seats, route.trim())
            rides.publish(draft, car, me, SUGGESTED_FARE)
            publishing = false
            published = true
        }
    }

    companion object {
        const val MAX_STOPS = 3
        const val SUGGESTED_FARE = 500L
    }
}
