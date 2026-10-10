package dev.liftonway.feature.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.liftonway.core.designsystem.component.IconAction
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.TextAction
import dev.liftonway.core.designsystem.component.TitleAlign
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.Jakarta
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.CardBrand
import dev.liftonway.core.model.PaymentCard
import dev.liftonway.core.model.Transaction
import dev.liftonway.core.model.TransactionType
import dev.liftonway.core.model.asPkr
import dev.liftonway.core.model.upperLabel
import java.util.Locale

@Composable
fun WalletRoute(onBack: () -> Unit, onHistory: () -> Unit, viewModel: WalletViewModel = hiltViewModel()) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    // The balance counts up to its new value after a top-up.
    val shown by animateFloatAsState(wallet.balance.toFloat(), tween(1100), label = "balance")

    Column(Modifier.fillMaxSize().background(LiftColors.Canvas)) {
        Column(Modifier.fillMaxWidth().background(Color.White), horizontalAlignment = Alignment.CenterHorizontally) {
            LiftTopBar("Wallet", onBack = onBack, align = TitleAlign.Center)
            Spacer(Modifier.height(30.dp))
            Text(
                shown.toLong().asPkr(),
                style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 36.sp, color = LiftColors.Ink),
                modifier = Modifier.entrance(0),
            )
            Text("Available credit", style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary, modifier = Modifier.entrance(1))
            Spacer(Modifier.height(56.dp))
            PrimaryButton("Top up", viewModel::openTopUp, Modifier.fillMaxWidth().padding(horizontal = 30.dp).entrance(2))
            Spacer(Modifier.height(18.dp))
            OutlineButton("History", onHistory, Modifier.fillMaxWidth().padding(horizontal = 30.dp).entrance(3))
            Spacer(Modifier.height(28.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 30.dp)) {
            Spacer(Modifier.height(20.dp))
            TextAction("+ADD CARD", viewModel::openAddCard, Modifier.offset(x = (-8).dp).entrance(4))
            Spacer(Modifier.height(12.dp))
            wallet.cards.forEachIndexed { i, card ->
                CardRow(card, { viewModel.removeCard(card) }, Modifier.entrance(5 + i))
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(24.dp).windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    viewModel.topUp?.let { step -> TopUpSheet(step, wallet.cards, viewModel::pay, viewModel::closeTopUp, onAddCard = viewModel::openAddCard) }
    if (viewModel.addingCard) AddCardSheet(viewModel.savingCard, viewModel::saveCard, viewModel::closeAddCard)
}

@Composable
private fun CardRow(card: PaymentCard, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(4.dp)).background(Color.White).padding(start = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(card.brand)
        Spacer(Modifier.width(30.dp))
        Text("•••• •••• •••• ${card.last4}", style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary, modifier = Modifier.weight(1f))
        Box {
            IconAction(LiftIcons.More, "Card options", { menu = true }, tint = LiftColors.TextSecondary)
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Remove card") }, onClick = {
                    menu = false
                    onRemove()
                })
            }
        }
    }
}

/** Two overlapping discs, as in the design, or the word VISA. */
@Composable
internal fun BrandMark(brand: CardBrand) {
    when (brand) {
        CardBrand.Mastercard -> Box(Modifier.size(width = 36.dp, height = 24.dp)) {
            Box(Modifier.size(24.dp).offset(x = 10.dp).clip(CircleShape).background(LiftColors.CardYellow))
            Box(Modifier.size(24.dp).clip(CircleShape).background(LiftColors.CardBlue))
        }
        CardBrand.Visa -> Text(
            "VISA",
            style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = LiftColors.CardBlue, letterSpacing = 0.5.sp),
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
fun TransactionHistoryRoute(onBack: () -> Unit, viewModel: WalletViewModel = hiltViewModel()) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(LiftColors.Canvas)) {
        LiftTopBar("Transaction history", Modifier.background(Color.White), onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            itemsIndexed(wallet.transactions, key = { _, it -> it.id }) { i, tx ->
                TransactionRow(tx, Modifier.animateItem().entrance(i.coerceAtMost(8), step = 40L))
            }
        }
    }
}

private data class TxLook(val icon: ImageVector, val color: Color)

private fun look(type: TransactionType) = when (type) {
    TransactionType.Received -> TxLook(LiftIcons.ArrowDownRight, LiftColors.Green)
    TransactionType.Sent -> TxLook(LiftIcons.ArrowUpLeft, LiftColors.Coral)
    TransactionType.TopUp -> TxLook(LiftIcons.Banknote, LiftColors.Amber)
}

@Composable
private fun TransactionRow(tx: Transaction, modifier: Modifier) {
    val look = look(tx.type)
    Row(modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(look.color), contentAlignment = Alignment.Center) {
            Icon(look.icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.date.upperLabel(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = LiftColors.TextSecondary)
            Text(tx.counterparty.uppercase(Locale.UK), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            (if (tx.type == TransactionType.Sent) "- " else "") + tx.amount.asPkr(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}
