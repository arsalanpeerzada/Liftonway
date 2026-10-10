package dev.liftonway.core.data

import dev.liftonway.core.model.Vehicle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The user's car. Having one is what makes the profile show the driver sections. */
@Singleton
class VehicleRepository @Inject constructor() {
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    suspend fun save(vehicle: Vehicle) {
        delay(700)
        _vehicle.value = vehicle
    }
}
