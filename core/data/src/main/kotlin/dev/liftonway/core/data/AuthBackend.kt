package dev.liftonway.core.data

import dev.liftonway.core.data.remote.CodeRequest
import dev.liftonway.core.data.remote.CodeVerify
import dev.liftonway.core.data.remote.LiftApi
import dev.liftonway.core.data.remote.MeDto
import dev.liftonway.core.data.remote.NameBody
import dev.liftonway.core.data.remote.DeviceBody
import dev.liftonway.core.data.remote.LogoutBody
import dev.liftonway.core.data.remote.SessionDto
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Who signed in: the tokens for later calls (none in the offline demo) and the account's name. */
data class SignedIn(val token: String?, val refreshToken: String?, val name: String)

/** The network half of signing in and managing the account. */
interface AuthBackend {
    suspend fun sendCode(countryCode: String, phone: String)

    /** Signs in, or signs up when [name] is given and the number is new. */
    suspend fun verify(countryCode: String, phone: String, code: String, name: String?): SignedIn
    suspend fun sendChangeCode(countryCode: String, phone: String)
    suspend fun verifyChange(countryCode: String, phone: String, code: String)
    suspend fun rename(name: String)

    /** Ends this device's session on the server and stops its pushes. Best effort: the app signs out locally either way. */
    suspend fun signOut(refreshToken: String, deviceToken: String?)

    /** Registers this install for push notifications. */
    suspend fun registerDevice(token: String)
}

@Singleton
class RemoteAuthBackend @Inject constructor(private val api: LiftApi) : AuthBackend {
    override suspend fun sendCode(countryCode: String, phone: String) = api.exec {
        method = HttpMethod.Post
        url("auth/otp")
        setBody(CodeRequest(countryCode, phone))
    }

    override suspend fun verify(countryCode: String, phone: String, code: String, name: String?): SignedIn {
        val session = try {
            api.call<SessionDto> {
                method = HttpMethod.Post
                url("auth/verify")
                setBody(CodeVerify(countryCode, phone, code, name))
            }
        } catch (e: ApiError) {
            // Signing in with a number that has no account yet.
            if (e.code == "NAME_REQUIRED") throw ApiError(e.code, "No account uses this number yet. Go back and sign up.")
            throw e
        }
        return SignedIn(session.token, session.refreshToken, session.user.name)
    }

    override suspend fun signOut(refreshToken: String, deviceToken: String?) = api.exec {
        method = HttpMethod.Post
        url("auth/logout")
        setBody(LogoutBody(refreshToken, deviceToken))
    }

    override suspend fun registerDevice(token: String) = api.exec {
        method = HttpMethod.Put
        url("me/device")
        setBody(DeviceBody(token))
    }

    override suspend fun sendChangeCode(countryCode: String, phone: String) = api.exec {
        method = HttpMethod.Post
        url("me/phone/otp")
        setBody(CodeRequest(countryCode, phone))
    }

    override suspend fun verifyChange(countryCode: String, phone: String, code: String) {
        api.call<MeDto> {
            method = HttpMethod.Post
            url("me/phone/verify")
            setBody(CodeVerify(countryCode, phone, code))
        }
    }

    override suspend fun rename(name: String) {
        api.call<MeDto> {
            method = HttpMethod.Patch
            url("me")
            setBody(NameBody(name))
        }
    }
}

/** The offline demo: no network, and any 4-digit code works. */
@Singleton
class FakeAuthBackend @Inject constructor() : AuthBackend {
    override suspend fun sendCode(countryCode: String, phone: String) = delay(700)

    override suspend fun verify(countryCode: String, phone: String, code: String, name: String?): SignedIn {
        delay(900)
        checkCode(code)
        return SignedIn(token = null, refreshToken = null, name = name ?: "Traveller")
    }

    override suspend fun sendChangeCode(countryCode: String, phone: String) = delay(700)

    override suspend fun verifyChange(countryCode: String, phone: String, code: String) {
        delay(900)
        checkCode(code)
    }

    override suspend fun rename(name: String) = delay(600)

    override suspend fun signOut(refreshToken: String, deviceToken: String?) = Unit

    override suspend fun registerDevice(token: String) = Unit

    private fun checkCode(code: String) {
        if (code.length != 4 || !code.all(Char::isDigit)) throw ApiError("INVALID_CODE", "That code didn't work.")
    }
}
