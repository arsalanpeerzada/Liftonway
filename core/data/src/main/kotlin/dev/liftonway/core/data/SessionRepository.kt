package dev.liftonway.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.liftonway.core.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

private val Context.sessionStore by preferencesDataStore(name = "session")

/**
 * Sign-up, sign-in and the profile. The prototype has no SMS service: any 4-digit code verifies.
 * Only the session flags and the profile are stored on the device, in DataStore.
 */
@Singleton
class SessionRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.sessionStore

    var pending: PendingVerification? = null
        private set

    val session: Flow<Session> = store.data.map { it.toSession() }

    suspend fun finishOnboarding() {
        store.edit { it[Keys.onboarded] = true }
    }

    fun startSignUp(name: String, countryCode: String, phone: String, photoUri: String?) {
        pending = PendingVerification.SignUp(name.trim(), countryCode, phone, photoUri)
    }

    fun startSignIn(countryCode: String, phone: String) {
        pending = PendingVerification.SignIn(countryCode, phone)
    }

    fun startChangeNumber(countryCode: String, phone: String) {
        pending = PendingVerification.ChangeNumber(countryCode, phone)
    }

    /** True when the code is accepted; the session is updated to match what was being verified. */
    suspend fun verify(code: String): Boolean {
        delay(900) // Long enough to see the button's progress state, as a real request would be.
        val target = pending ?: return false
        if (code.length != OTP_LENGTH || !code.all(Char::isDigit)) return false
        store.edit { prefs ->
            when (target) {
                is PendingVerification.SignUp -> {
                    prefs[Keys.name] = target.name
                    prefs[Keys.countryCode] = target.countryCode
                    prefs[Keys.phone] = target.phone
                    target.photoUri?.let { prefs[Keys.photo] = it } ?: prefs.remove(Keys.photo)
                    prefs[Keys.signedIn] = true
                }
                is PendingVerification.SignIn -> {
                    // A returning user on a fresh install has no stored name yet.
                    if (prefs[Keys.phone] != target.phone) {
                        prefs[Keys.name] = prefs[Keys.name] ?: "Traveller"
                        prefs[Keys.countryCode] = target.countryCode
                        prefs[Keys.phone] = target.phone
                    }
                    prefs[Keys.signedIn] = true
                }
                is PendingVerification.ChangeNumber -> {
                    prefs[Keys.countryCode] = target.countryCode
                    prefs[Keys.phone] = target.phone
                }
            }
        }
        pending = null
        return true
    }

    suspend fun updateProfile(name: String, photoUri: String?) {
        store.edit { prefs ->
            prefs[Keys.name] = name.trim()
            photoUri?.let { prefs[Keys.photo] = it } ?: prefs.remove(Keys.photo)
        }
    }

    suspend fun signOut() {
        store.edit { it[Keys.signedIn] = false }
    }

    private fun Preferences.toSession(): Session {
        val name = this[Keys.name]
        val phone = this[Keys.phone]
        val profile = if (name != null && phone != null) {
            UserProfile(name, this[Keys.countryCode] ?: "+92", phone, this[Keys.photo])
        } else {
            null
        }
        return Session(onboarded = this[Keys.onboarded] == true, signedIn = this[Keys.signedIn] == true, profile = profile)
    }

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val signedIn = booleanPreferencesKey("signed_in")
        val name = stringPreferencesKey("name")
        val countryCode = stringPreferencesKey("country_code")
        val phone = stringPreferencesKey("phone")
        val photo = stringPreferencesKey("photo")
    }

    companion object {
        const val OTP_LENGTH = 4
    }
}
