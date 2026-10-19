package dev.liftonway.core.data

import dev.liftonway.core.data.remote.LiftApi
import dev.liftonway.core.data.remote.NotificationDto
import dev.liftonway.core.data.remote.PakistanTime
import dev.liftonway.core.data.remote.toModel
import dev.liftonway.core.model.AppNotification
import dev.liftonway.core.model.NotificationKind
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AnswerResult {
    data object Done : AnswerResult
    data object NotEnoughCredit : AnswerResult
}

/** The notifications list. Calls throw [ApiError] when they fail; [items] keeps the last loaded list. */
interface NotificationRepository {
    val items: StateFlow<List<AppNotification>>

    fun now(): LocalDateTime

    suspend fun refresh()

    /** Says yes or no to a seat or payment request. */
    suspend fun answer(id: String, accept: Boolean): AnswerResult

    suspend fun remove(id: String)

    suspend fun markAllRead()
}

/** The offline demo's notifications. Confirming a payment request pays it from the in-memory wallet. */
@Singleton
class FakeNotificationRepository @Inject constructor(
    private val clock: Clock,
    private val wallet: WalletRepository,
) : NotificationRepository {
    private val _items = MutableStateFlow(seed())
    override val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    override fun now(): LocalDateTime = LocalDateTime.now(clock)

    override suspend fun refresh() = Unit

    override suspend fun answer(id: String, accept: Boolean): AnswerResult {
        val item = _items.value.first { it.id == id }
        val outcome = when {
            !accept -> "Declined"
            item.kind == NotificationKind.PaymentRequest -> {
                if (!wallet.pay(item.personName, item.amount ?: 0)) return AnswerResult.NotEnoughCredit
                "Paid"
            }
            else -> {
                delay(600)
                "Accepted"
            }
        }
        _items.update { list -> list.map { if (it.id == id) it.copy(actionable = false, outcome = outcome) else it } }
        return AnswerResult.Done
    }

    override suspend fun remove(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun markAllRead() {
        _items.update { list -> list.map { it.copy(read = true) } }
    }

    private fun seed(): List<AppNotification> {
        val now = LocalDateTime.now(clock)
        return listOf(
            AppNotification("n1", NotificationKind.RideRequest, "Ayesha Malik", "wants to have a ride with you", now.minusHours(1), actionable = true),
            AppNotification("n2", NotificationKind.PaymentRequest, "Hamza Tariq", "requested a payment for your ride", now.minusHours(1), amount = 450, actionable = true),
            AppNotification("n3", NotificationKind.PaymentSent, "Bilal Ahmed", "was sent PKR 200 from your wallet", now.minusHours(3), amount = 200),
            AppNotification("n4", NotificationKind.PaymentReceived, "Sana Iqbal", "sent you PKR 200 to your wallet", now.minusDays(1), amount = 200),
        )
    }
}

/** Notifications from the API. Seat requests are answered through the booking, then both lists reload. */
@Singleton
class RemoteNotificationRepository @Inject constructor(
    private val api: LiftApi,
    private val rides: RideRepository,
    private val clock: Clock,
) : NotificationRepository {
    private val _items = MutableStateFlow<List<AppNotification>>(emptyList())
    override val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    override fun now(): LocalDateTime = LocalDateTime.now(clock.withZone(PakistanTime))

    override suspend fun refresh() {
        _items.value = api.call<List<NotificationDto>> { url("me/notifications") }.mapNotNull { it.toModel() }
    }

    override suspend fun answer(id: String, accept: Boolean): AnswerResult {
        val item = _items.value.first { it.id == id }
        val booking = requireNotNull(item.bookingId) { "Only seat requests can be answered" }
        try {
            rides.answer(booking, accept)
        } finally {
            // Whatever happened (say, the passenger withdrew meanwhile), show the server's current state.
            runCatching { refresh() }
        }
        return AnswerResult.Done
    }

    override suspend fun remove(id: String) {
        val before = _items.value
        _items.update { list -> list.filterNot { it.id == id } }
        try {
            api.exec {
                method = HttpMethod.Delete
                url("me/notifications/$id")
            }
        } catch (e: ApiError) {
            if (e.code != "NOT_FOUND") {
                _items.value = before
                throw e
            }
        }
    }

    override suspend fun markAllRead() {
        if (_items.value.none { !it.read }) return
        _items.update { list -> list.map { it.copy(read = true) } }
        api.exec {
            method = HttpMethod.Post
            url("me/notifications/read")
        }
    }
}
