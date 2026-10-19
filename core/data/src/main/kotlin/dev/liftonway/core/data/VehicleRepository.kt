package dev.liftonway.core.data

import dev.liftonway.core.data.remote.LiftApi
import dev.liftonway.core.data.remote.VehicleDto
import dev.liftonway.core.data.remote.toDto
import dev.liftonway.core.data.remote.toModel
import dev.liftonway.core.model.Vehicle
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The user's car. Having one is what makes the profile show the driver sections. */
interface VehicleRepository {
    val vehicle: StateFlow<Vehicle?>

    suspend fun save(vehicle: Vehicle)
    suspend fun refresh()
}

@Singleton
class RemoteVehicleRepository @Inject constructor(private val api: LiftApi) : VehicleRepository {
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    override val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    override suspend fun save(vehicle: Vehicle) {
        _vehicle.value = api.call<VehicleDto> {
            method = HttpMethod.Put
            url("me/vehicle")
            setBody(vehicle.toDto())
        }.toModel()
    }

    override suspend fun refresh() {
        _vehicle.value = try {
            api.call<VehicleDto> { url("me/vehicle") }.toModel()
        } catch (e: ApiError) {
            if (e.code == "NOT_FOUND") null else throw e
        }
    }
}

@Singleton
class FakeVehicleRepository @Inject constructor() : VehicleRepository {
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    override val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    override suspend fun save(vehicle: Vehicle) {
        delay(700)
        _vehicle.value = vehicle
    }

    override suspend fun refresh() = Unit
}
