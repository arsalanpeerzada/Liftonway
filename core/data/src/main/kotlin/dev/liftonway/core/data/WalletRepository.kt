package dev.liftonway.core.data

import dev.liftonway.core.model.CardBrand
import dev.liftonway.core.model.PaymentCard
import dev.liftonway.core.model.Rupees
import dev.liftonway.core.model.Transaction
import dev.liftonway.core.model.TransactionType
import dev.liftonway.core.model.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credit, saved cards and history, in memory.
 * A real build hands card details to the payment provider's SDK and keeps only its token; here the full
 * number never leaves the add-card sheet, and only the brand and last four digits are kept.
 */
@Singleton
class WalletRepository @Inject constructor(private val clock: Clock) {
    private val _wallet = MutableStateFlow(seed())
    val wallet: StateFlow<Wallet> = _wallet.asStateFlow()

    suspend fun topUp(amount: Rupees, cardId: String) {
        require(amount > 0)
        require(_wallet.value.cards.any { it.id == cardId })
        delay(1200)
        record(amount, "Top up", TransactionType.TopUp)
    }

    suspend fun addCard(number: String): PaymentCard {
        val digits = number.filter(Char::isDigit)
        delay(900)
        val card = PaymentCard(UUID.randomUUID().toString(), if (digits.startsWith("4")) CardBrand.Visa else CardBrand.Mastercard, digits.takeLast(4))
        _wallet.update { it.copy(cards = it.cards + card) }
        return card
    }

    fun removeCard(id: String) {
        _wallet.update { w -> w.copy(cards = w.cards.filterNot { it.id == id }) }
    }

    /** Pays from credit; false when the balance doesn't cover it. */
    suspend fun pay(to: String, amount: Rupees): Boolean {
        delay(800)
        if (_wallet.value.balance < amount) return false
        record(-amount, to, TransactionType.Sent)
        return true
    }

    private fun record(signedAmount: Rupees, counterparty: String, type: TransactionType) {
        _wallet.update {
            val tx = Transaction(UUID.randomUUID().toString(), counterparty, kotlin.math.abs(signedAmount), type, LocalDate.now(clock))
            it.copy(balance = it.balance + signedAmount, transactions = listOf(tx) + it.transactions)
        }
    }

    private fun seed(): Wallet {
        val day = LocalDate.now(clock).minusDays(17)
        val people = listOf("Ayesha Malik", "Hamza Tariq", "Bilal Ahmed", "Sana Iqbal")
        val history = (0 until 8).map { i ->
            val type = when (i % 3) {
                0 -> TransactionType.Received
                1 -> TransactionType.Sent
                else -> TransactionType.TopUp
            }
            Transaction("t$i", if (type == TransactionType.TopUp) "Top up" else people[i % people.size], if (i % 2 == 0) 2000 else 1000, type, day.minusDays(i * 2L))
        }
        return Wallet(balance = 0, cards = listOf(PaymentCard("c1", CardBrand.Mastercard, "2323")), transactions = history)
    }
}
