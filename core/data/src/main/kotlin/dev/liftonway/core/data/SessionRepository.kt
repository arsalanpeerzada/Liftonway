package dev.liftonway.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.liftonway.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Session(val onboarded: Boolean, val signedIn: Boolean, val profile: UserProfile?)

/** Who is waiting on a one-time code, and why. */
sealed interface PendingVerification {
    val countryCode: String
    val phone: String

    data class SignUp(val name: String, override val countryCode: String, override val phone: String, val photoUri: String?) : PendingVerification
    data class SignIn(override val countryCode: String, override val phone: String) : PendingVerification
    data class ChangeNumber(override val countryCode: String, override val phone: String) : PendingVerification
}

/**
 * Sign-up, sign-in and the profile. Codes go through [AuthBackend] (the API, or the offline demo); the session
 * flags, the profile and the token are kept on the device in DataStore. The profile photo stays on the device:
 * the API has no photo upload yet.
 */
@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: AuthBackend,
    private val tokens: TokenStore,
) {
    private val store = context.sessionStore

    var pending: PendingVerification? = null
        private set

    val session: Flow<Session> = store.data.map { it.toSession() }

    suspend fun finishOnboarding() {
        store.edit { it[SessionKeys.onboarded] = true }
    }

    /** Sends a code; throws [ApiError] (for example when asked again too soon). */
    suspend fun startSignUp(name: String, countryCode: String, phone: String, photoUri: String?) {
        auth.sendCode(countryCode, phone)
        pending = PendingVerification.SignUp(name.trim(), countryCode, phone, photoUri)
    }

    suspend fun startSignIn(countryCode: String, phone: String) {
        auth.sendCode(countryCode, phone)
        pending = PendingVerification.SignIn(countryCode, phone)
    }

    suspend fun startChangeNumber(countryCode: String, phone: String) {
        auth.sendChangeCode(countryCode, phone)
        pending = PendingVerification.ChangeNumber(countryCode, phone)
    }

    /** Checks the code for whatever is pending and updates the session; throws [ApiError] when it fails. */
    suspend fun verify(code: String) {
        val target = pending ?: throw ApiError("CODE_NOT_REQUESTED", "Ask for a new code.")
        when (target) {
            is PendingVerification.SignUp -> {
                val signedIn = auth.verify(target.countryCode, target.phone, code, target.name)
                saveSignedIn(signedIn, target.countryCode, target.phone, target.photoUri)
            }
            is PendingVerification.SignIn -> {
                val signedIn = auth.verify(target.countryCode, target.phone, code, name = null)
                saveSignedIn(signedIn, target.countryCode, target.phone, photoUri = store.data.first()[SessionKeys.photo])
            }
            is PendingVerification.ChangeNumber -> {
                auth.verifyChange(target.countryCode, target.phone, code)
                store.edit { prefs ->
                    prefs[SessionKeys.countryCode] = target.countryCode
                    prefs[SessionKeys.phone] = target.phone
                }
            }
        }
        pending = null
    }

    private suspend fun saveSignedIn(signedIn: SignedIn, countryCode: String, phone: String, photoUri: String?) {
        if (signedIn.token != null && signedIn.refreshToken != null) tokens.update(signedIn.token, signedIn.refreshToken) else tokens.clear()
        store.edit { prefs ->
            prefs[SessionKeys.name] = signedIn.name
            prefs[SessionKeys.countryCode] = countryCode
            prefs[SessionKeys.phone] = phone
            photoUri?.let { prefs[SessionKeys.photo] = it } ?: prefs.remove(SessionKeys.photo)
            prefs[SessionKeys.signedIn] = true
        }
    }

    suspend fun updateProfile(name: String, photoUri: String?) {
        auth.rename(name.trim())
        store.edit { prefs ->
            prefs[SessionKeys.name] = name.trim()
            photoUri?.let { prefs[SessionKeys.photo] = it } ?: prefs.remove(SessionKeys.photo)
        }
    }

    /** Signs out here at once, then tells the server; if that fails (say, offline) the session lapses on its own. */
    suspend fun signOut() {
        val refresh = tokens.refreshToken()
        val device = store.data.first()[SessionKeys.deviceToken]
        tokens.clear()
        store.edit { it[SessionKeys.signedIn] = false }
        if (refresh != null) {
            try {
                auth.signOut(refresh, device)
            } catch (_: ApiError) {
                // Nothing to show: the user is already signed out on this device.
            }
        }
    }

    /**
     * Tells the server where to push for the signed-in user. Called after sign-in and whenever Firebase rotates
     * the token; does nothing while signed out (the next sign-in registers it).
     */
    suspend fun registerDevice(token: String) {
        store.edit { it[SessionKeys.deviceToken] = token }
        if (store.data.first()[SessionKeys.signedIn] == true) auth.registerDevice(token)
    }

    /** Whether the app has already asked for permission to show notifications (asked once, after sign-in). */
    suspend fun askedNotifications(): Boolean = store.data.first()[SessionKeys.askedNotifications] == true

    suspend fun markAskedNotifications() {
        store.edit { it[SessionKeys.askedNotifications] = true }
    }

    private fun Preferences.toSession(): Session {
        val name = this[SessionKeys.name]
        val phone = this[SessionKeys.phone]
        val profile = if (name != null && phone != null) {
            UserProfile(name, this[SessionKeys.countryCode] ?: "+92", phone, this[SessionKeys.photo])
        } else {
            null
        }
        return Session(onboarded = this[SessionKeys.onboarded] == true, signedIn = this[SessionKeys.signedIn] == true, profile = profile)
    }

    companion object {
        const val OTP_LENGTH = 4
    }
}
