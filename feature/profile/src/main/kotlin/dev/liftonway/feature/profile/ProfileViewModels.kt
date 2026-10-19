package dev.liftonway.feature.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.PhotoStore
import dev.liftonway.core.data.SessionRepository
import dev.liftonway.core.data.VehicleRepository
import dev.liftonway.core.model.UserProfile
import dev.liftonway.core.model.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val session: SessionRepository, vehicles: VehicleRepository) : ViewModel() {
    val profile: StateFlow<UserProfile?> = session.session.map { it.profile }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val vehicle: StateFlow<Vehicle?> = vehicles.vehicle

    init {
        // The driver sections depend on having a vehicle, which lives on the server.
        viewModelScope.launch { runCatching { vehicles.refresh() } }
    }

    fun signOut(then: () -> Unit) {
        viewModelScope.launch {
            session.signOut()
            then()
        }
    }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(private val session: SessionRepository, private val photos: PhotoStore) : ViewModel() {
    var name by mutableStateOf("")
    var photoUri by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var failure by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            session.session.first().profile?.let {
                name = it.name
                photoUri = it.photoUri
            }
            loaded = true
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch { photoUri = photos.keep(uri) }
    }

    fun save(then: () -> Unit) {
        if (name.isBlank() || saving) return
        viewModelScope.launch {
            saving = true
            failure = null
            try {
                session.updateProfile(name, photoUri)
                then()
            } catch (e: ApiError) {
                failure = e.message
            } finally {
                saving = false
            }
        }
    }
}

@HiltViewModel
class ChangeNumberViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    /** False while confirming the current number, true while entering the new one. */
    var enteringNew by mutableStateOf(false)
        private set
    var number by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    fun onNumber(value: String) {
        number = value.filter(Char::isDigit).take(11)
        error = null
    }

    fun back(onExit: () -> Unit) {
        if (enteringNew) {
            enteringNew = false
            number = ""
            error = null
        } else {
            onExit()
        }
    }

    fun next(onCodeSent: () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            val profile = session.session.first().profile ?: return@launch
            if (!enteringNew) {
                if (number != profile.phone) {
                    error = "That isn’t the number on your account."
                    return@launch
                }
                enteringNew = true
                number = ""
            } else {
                if (number.length < 10 || number == profile.phone) {
                    error = if (number == profile.phone) "That’s already your number." else "Enter a full phone number."
                    return@launch
                }
                busy = true
                try {
                    session.startChangeNumber(profile.countryCode, number)
                    onCodeSent()
                } catch (e: ApiError) {
                    error = e.message
                } finally {
                    busy = false
                }
            }
        }
    }
}

@HiltViewModel
class VehicleViewModel @Inject constructor(private val vehicles: VehicleRepository, private val photos: PhotoStore) : ViewModel() {
    private val existing = vehicles.vehicle.value

    /** A saved vehicle opens read-only until Edit is tapped. */
    var editing by mutableStateOf(existing == null)
        private set
    val isNew = existing == null
    var name by mutableStateOf(existing?.name.orEmpty())
    var model by mutableStateOf(existing?.model.orEmpty())
    var plate by mutableStateOf(existing?.plate.orEmpty())
    var color by mutableStateOf(existing?.color.orEmpty())
    var airConditioning by mutableStateOf(existing?.airConditioning ?: false)
    var heater by mutableStateOf(existing?.heater ?: false)
    var photoUri by mutableStateOf(existing?.photoUri)
        private set
    var showErrors by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var failure by mutableStateOf<String?>(null)
        private set

    fun edit() {
        editing = true
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch { photoUri = photos.keep(uri) }
    }

    fun missing(value: String) = showErrors && value.isBlank()

    fun save(then: () -> Unit) {
        showErrors = true
        if (listOf(name, model, plate, color).any { it.isBlank() } || saving) return
        viewModelScope.launch {
            saving = true
            failure = null
            try {
                vehicles.save(Vehicle(name.trim(), model.trim(), plate.trim().uppercase(), color.trim(), airConditioning, heater, photoUri))
                then()
            } catch (e: ApiError) {
                failure = e.message
            } finally {
                saving = false
            }
        }
    }
}
