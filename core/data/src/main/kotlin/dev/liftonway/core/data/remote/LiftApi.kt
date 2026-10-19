package dev.liftonway.core.data.remote

import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.BuildConfig
import dev.liftonway.core.data.TokenSource
import dev.liftonway.core.data.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Lift On Way API over HTTP. Adds the bearer token, turns problem responses into [ApiError] with the
 * server's message, and signs the user out when the server stops accepting their token.
 */
@Singleton
class LiftApi internal constructor(@PublishedApi internal val tokens: TokenSource, engine: HttpClientEngine?) {
    @Inject constructor(tokens: TokenStore) : this(tokens as TokenSource, null)

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @PublishedApi
    internal val client: HttpClient = (if (engine != null) HttpClient(engine) { configure() } else HttpClient(OkHttp) { configure() })

    private fun io.ktor.client.HttpClientConfig<*>.configure() {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
        defaultRequest {
            url(BuildConfig.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }

    suspend inline fun <reified T> call(crossinline block: HttpRequestBuilder.() -> Unit): T = send(block).body()

    /** For calls whose response body the app doesn't need. */
    suspend inline fun exec(crossinline block: HttpRequestBuilder.() -> Unit) {
        send(block)
    }

    @PublishedApi
    internal suspend inline fun send(crossinline block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val token = tokens.token()
        var response = execute(token) { block() }
        if (response.status == HttpStatusCode.Unauthorized && token != null) {
            // The access token ran out: renew it once and repeat the call.
            val renewed = renew(failed = token) ?: throw ApiError(SIGNED_OUT, "Your session has ended. Please sign in again.")
            response = execute(renewed) { block() }
            if (response.status == HttpStatusCode.Unauthorized) tokens.expire()
        }
        if (response.status.isSuccess()) return response
        val problem = runCatching { response.body<Problem>() }.getOrNull()
        throw ApiError(problem?.code ?: ApiError.UNKNOWN, problem?.detail ?: "Something went wrong. Please try again.")
    }

    @PublishedApi
    internal suspend inline fun execute(token: String?, crossinline block: HttpRequestBuilder.() -> Unit): HttpResponse = try {
        client.request {
            token?.let { bearerAuth(it) }
            block()
        }
    } catch (e: IOException) {
        throw ApiError(ApiError.OFFLINE, "Can't reach Lift On Way. Check your connection and try again.")
    }

    private val renewing = Mutex()

    /**
     * Trades the refresh token for a new pair. One renewal at a time: calls that failed together wait here and
     * reuse the first renewal, because presenting the same refresh token twice would end the session.
     * Null when the server ended the session (the user is signed out); offline and server errors are thrown.
     */
    @PublishedApi
    internal suspend fun renew(failed: String): String? = renewing.withLock {
        tokens.token()?.let { current -> if (current != failed) return current }
        val refresh = tokens.refreshToken()
        if (refresh == null) {
            tokens.expire()
            return null
        }
        val response = execute(token = null) {
            method = HttpMethod.Post
            url("auth/refresh")
            setBody(RefreshBody(refresh))
        }
        when {
            response.status.isSuccess() -> {
                val renewed = response.body<RenewedDto>()
                tokens.update(renewed.token, renewed.refreshToken)
                renewed.token
            }
            response.status.value in 400..499 -> {
                tokens.expire()
                null
            }
            else -> throw ApiError(ApiError.UNKNOWN, "Something went wrong. Please try again.")
        }
    }

    companion object {
        const val SIGNED_OUT = "SIGNED_OUT"
    }
}
