package dev.liftonway.feature.auth

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.PendingVerification
import dev.liftonway.core.data.PhotoStore
import dev.liftonway.core.data.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PHONE_DIGITS = 10

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val session: SessionRepository,
    private val photos: PhotoStore,
) : ViewModel() {
    var name by mutableStateOf("")
    var countryCode by mutableStateOf("+92")
    var phone by mutableStateOf("")
    var photoUri by mutableStateOf<String?>(null)
        private set
    var showErrors by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set

    val nameError get() = showErrors && name.isBlank()
    val phoneError get() = showErrors && phone.length < MIN_PHONE_DIGITS

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch { photoUri = photos.keep(uri) }
    }

    fun submit(onCodeSent: () -> Unit) {
        showErrors = true
        if (name.isBlank() || phone.length < MIN_PHONE_DIGITS || sending) return
        viewModelScope.launch {
            sending = true
            delay(700) // Stands in for the request that sends the SMS.
            session.startSignUp(name, countryCode, phone, photoUri)
            sending = false
            onCodeSent()
        }
    }
}

@HiltViewModel
class SignInViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    var countryCode by mutableStateOf("+92")
    var phone by mutableStateOf("")
    var showErrors by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set

    val phoneError get() = showErrors && phone.length < MIN_PHONE_DIGITS

    fun submit(onCodeSent: () -> Unit) {
        showErrors = true
        if (phone.length < MIN_PHONE_DIGITS || sending) return
        viewModelScope.launch {
            sending = true
            delay(700)
            session.startSignIn(countryCode, phone)
            sending = false
            onCodeSent()
        }
    }
}

@HiltViewModel
class OtpViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    /** Null when the process restarted mid-flow and there's nothing left to verify. */
    val pending: PendingVerification? = session.pending

    var code by mutableStateOf("")
        private set
    var error by mutableStateOf(false)
        private set
    var verifying by mutableStateOf(false)
        private set

    fun onCode(value: String) {
        code = value.filter(Char::isDigit).take(SessionRepository.OTP_LENGTH)
        error = false
    }

    fun verify(onVerified: () -> Unit) {
        if (verifying) return
        viewModelScope.launch {
            verifying = true
            val ok = session.verify(code)
            verifying = false
            if (ok) onVerified() else error = true
        }
    }
}
