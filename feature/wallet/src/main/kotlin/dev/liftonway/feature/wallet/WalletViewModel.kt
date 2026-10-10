package dev.liftonway.feature.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.WalletRepository
import dev.liftonway.core.model.PaymentCard
import dev.liftonway.core.model.Rupees
import dev.liftonway.core.model.Wallet
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TopUpStep {
    data object Form : TopUpStep
    data object Paying : TopUpStep
    data class Done(val amount: Rupees) : TopUpStep
}

@HiltViewModel
class WalletViewModel @Inject constructor(private val repository: WalletRepository) : ViewModel() {
    val wallet: StateFlow<Wallet> = repository.wallet

    var topUp by mutableStateOf<TopUpStep?>(null)
        private set
    var addingCard by mutableStateOf(false)
        private set
    var savingCard by mutableStateOf(false)
        private set

    fun openTopUp() {
        topUp = TopUpStep.Form
    }

    fun closeTopUp() {
        if (topUp != TopUpStep.Paying) topUp = null
    }

    fun pay(amount: Rupees, card: PaymentCard) {
        viewModelScope.launch {
            topUp = TopUpStep.Paying
            repository.topUp(amount, card.id)
            topUp = TopUpStep.Done(amount)
        }
    }

    fun openAddCard() {
        addingCard = true
    }

    fun closeAddCard() {
        if (!savingCard) addingCard = false
    }

    /** The number is passed straight through and not kept here; only the repository's last four digits survive. */
    fun saveCard(number: String, then: () -> Unit) {
        viewModelScope.launch {
            savingCard = true
            repository.addCard(number)
            savingCard = false
            then()
        }
    }

    fun removeCard(card: PaymentCard) = repository.removeCard(card.id)
}
