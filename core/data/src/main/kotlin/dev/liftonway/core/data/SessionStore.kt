package dev.liftonway.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** One DataStore file for the session: flags, the cached profile and the API token. App-private storage. */
internal val Context.sessionStore by preferencesDataStore(name = "session")

internal object SessionKeys {
    val onboarded = booleanPreferencesKey("onboarded")
    val signedIn = booleanPreferencesKey("signed_in")
    val name = stringPreferencesKey("name")
    val countryCode = stringPreferencesKey("country_code")
    val phone = stringPreferencesKey("phone")
    val photo = stringPreferencesKey("photo")
    val token = stringPreferencesKey("token")
}

/** Where the API client gets the token from, and how it reports that the server rejected it. */
interface TokenSource {
    suspend fun token(): String?
    suspend fun expire()
}

/** The API token, cached in memory after the first read. */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) : TokenSource {
    private val store = context.sessionStore

    @Volatile private var cached: String? = null

    @Volatile private var loaded = false

    override suspend fun token(): String? {
        if (!loaded) {
            cached = store.data.first()[SessionKeys.token]
            loaded = true
        }
        return cached
    }

    suspend fun set(token: String?) {
        store.edit { if (token == null) it.remove(SessionKeys.token) else it[SessionKeys.token] = token }
        cached = token
        loaded = true
    }

    /** The server no longer accepts the token: forget it and show sign-in again. */
    override suspend fun expire() {
        store.edit {
            it.remove(SessionKeys.token)
            it[SessionKeys.signedIn] = false
        }
        cached = null
        loaded = true
    }
}
