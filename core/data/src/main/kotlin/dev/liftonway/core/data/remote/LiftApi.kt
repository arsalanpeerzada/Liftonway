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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
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
        val response = try {
            client.request {
                token?.let { bearerAuth(it) }
                block()
            }
        } catch (e: IOException) {
            throw ApiError(ApiError.OFFLINE, "Can't reach Lift On Way. Check your connection and try again.")
        }
        if (response.status.isSuccess()) return response
        if (response.status == HttpStatusCode.Unauthorized && token != null) tokens.expire()
        val problem = runCatching { response.body<Problem>() }.getOrNull()
        throw ApiError(problem?.code ?: ApiError.UNKNOWN, problem?.detail ?: "Something went wrong. Please try again.")
    }
}
