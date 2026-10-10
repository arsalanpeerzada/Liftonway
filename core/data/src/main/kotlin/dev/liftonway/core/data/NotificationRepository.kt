package dev.liftonway.core.data

import dev.liftonway.core.model.AppNotification
import dev.liftonway.core.model.NotificationKind
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

/** The notifications list. Confirming a payment request pays it from the wallet. */
@Singleton
class NotificationRepository @Inject constructor(
    private val clock: Clock,
    private val wallet: WalletRepository,
) {
    private val _items = MutableStateFlow(seed())
    val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    fun now(): LocalDateTime = LocalDateTime.now(clock)

    suspend fun answer(id: String, accept: Boolean): AnswerResult {
        val item = _items.value.first { it.id == id }
        if (accept && item.kind == NotificationKind.PaymentRequest) {
            if (!wallet.pay(item.personName, item.amount ?: 0)) return AnswerResult.NotEnoughCredit
        } else {
            delay(600)
        }
        _items.update { list -> list.map { if (it.id == id) it.copy(answered = true) else it } }
        return AnswerResult.Done
    }

    fun remove(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    private fun seed(): List<AppNotification> {
        val now = LocalDateTime.now(clock)
        return listOf(
            AppNotification("n1", NotificationKind.RideRequest, "Ayesha Malik", "wants to have a ride with you", now.minusHours(1)),
            AppNotification("n2", NotificationKind.PaymentRequest, "Hamza Tariq", "requested a payment for your ride", now.minusHours(1), amount = 450),
            AppNotification("n3", NotificationKind.PaymentSent, "Bilal Ahmed", "was sent PKR 200 from your wallet", now.minusHours(3), amount = 200),
            AppNotification("n4", NotificationKind.PaymentReceived, "Sana Iqbal", "sent you PKR 200 to your wallet", now.minusDays(1), amount = 200),
        )
    }
}
