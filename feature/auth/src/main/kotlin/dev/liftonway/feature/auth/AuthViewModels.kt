package dev.liftonway.feature.auth

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.PendingVerification
import dev.liftonway.core.data.PhotoStore
import dev.liftonway.core.data.SessionRepository
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

    /** What went wrong on the last attempt, from the server or the connection. */
    var failure by mutableStateOf<String?>(null)
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
            failure = null
            try {
                session.startSignUp(name, countryCode, phone, photoUri)
                onCodeSent()
            } catch (e: ApiError) {
                failure = e.message
            } finally {
                sending = false
            }
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
    var failure by mutableStateOf<String?>(null)
        private set

    val phoneError get() = showErrors && phone.length < MIN_PHONE_DIGITS

    fun submit(onCodeSent: () -> Unit) {
        showErrors = true
        if (phone.length < MIN_PHONE_DIGITS || sending) return
        viewModelScope.launch {
            sending = true
            failure = null
            try {
                session.startSignIn(countryCode, phone)
                onCodeSent()
            } catch (e: ApiError) {
                failure = e.message
            } finally {
                sending = false
            }
        }
    }
}

@HiltViewModel
class OtpViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    /** Null when the process restarted mid-flow and there's nothing left to verify. */
    val pending: PendingVerification? = session.pending

    var code by mutableStateOf("")
        private set
    /** The reason the last code was refused, ready to show. */
    var error by mutableStateOf<String?>(null)
        private set
    var verifying by mutableStateOf(false)
        private set

    fun onCode(value: String) {
        code = value.filter(Char::isDigit).take(SessionRepository.OTP_LENGTH)
        error = null
    }

    fun verify(onVerified: () -> Unit) {
        if (verifying) return
        viewModelScope.launch {
            verifying = true
            try {
                session.verify(code)
                onVerified()
            } catch (e: ApiError) {
                error = e.message
            } finally {
                verifying = false
            }
        }
    }
}
