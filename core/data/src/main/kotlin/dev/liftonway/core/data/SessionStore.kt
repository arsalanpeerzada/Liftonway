package dev.liftonway.core.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** One DataStore file for the session: flags, the cached profile and the encrypted API tokens. App-private storage. */
internal val Context.sessionStore by preferencesDataStore(name = "session")

internal object SessionKeys {
    val onboarded = booleanPreferencesKey("onboarded")
    val signedIn = booleanPreferencesKey("signed_in")
    val name = stringPreferencesKey("name")
    val countryCode = stringPreferencesKey("country_code")
    val phone = stringPreferencesKey("phone")
    val photo = stringPreferencesKey("photo")
    val accessToken = stringPreferencesKey("access_token_sealed")
    val refreshToken = stringPreferencesKey("refresh_token_sealed")

    /** Plain token from builds before encryption; removed on sight. */
    val legacyToken = stringPreferencesKey("token")
}

/** Where the API client gets tokens from, stores renewed ones, and reports that the server ended the session. */
interface TokenSource {
    suspend fun token(): String?
    suspend fun refreshToken(): String?
    suspend fun update(access: String, refresh: String)
    suspend fun expire()
}

/**
 * The access and refresh tokens, encrypted with a key that never leaves the Android Keystore, and cached in
 * memory after the first read. If the key is gone (say, data copied to another device), the session ends.
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) : TokenSource {
    private val store = context.sessionStore
    private val box = KeystoreBox("liftonway.session")

    private data class Pair(val access: String?, val refresh: String?)

    @Volatile private var cached: Pair? = null

    private suspend fun load(): Pair {
        cached?.let { return it }
        val prefs = store.data.first()
        val access = prefs[SessionKeys.accessToken]?.let(box::open)
        val refresh = prefs[SessionKeys.refreshToken]?.let(box::open)
        val unreadable = (prefs[SessionKeys.accessToken] != null && access == null) || (prefs[SessionKeys.refreshToken] != null && refresh == null)
        // Older builds kept a plain token and no refresh token; those sessions simply sign in again.
        if (unreadable || prefs[SessionKeys.legacyToken] != null) {
            expire()
            return Pair(null, null)
        }
        return Pair(access, refresh).also { cached = it }
    }

    override suspend fun token(): String? = load().access

    override suspend fun refreshToken(): String? = load().refresh

    override suspend fun update(access: String, refresh: String) {
        store.edit {
            it[SessionKeys.accessToken] = box.seal(access)
            it[SessionKeys.refreshToken] = box.seal(refresh)
        }
        cached = Pair(access, refresh)
    }

    /** Forgets the tokens without touching the signed-in flag (the caller decides what the user sees). */
    suspend fun clear() {
        store.edit {
            it.remove(SessionKeys.accessToken)
            it.remove(SessionKeys.refreshToken)
            it.remove(SessionKeys.legacyToken)
        }
        cached = Pair(null, null)
    }

    /** The server ended the session: forget the tokens and show sign-in again. */
    override suspend fun expire() {
        clear()
        store.edit { it[SessionKeys.signedIn] = false }
    }
}

/** AES-GCM with a 256-bit key generated inside the Android Keystore; the key material can't be read out. */
internal class KeystoreBox(private val alias: String) {
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).apply { init(spec) }.generateKey()
    }

    fun seal(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }

    /** Null when the value can't be decrypted, for example because the key was reset. */
    fun open(sealed: String): String? = runCatching {
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_BYTES)) }
        String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
    }.getOrNull()

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
