package dev.liftonway.feature.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTextField
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.TextAction
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes
import dev.liftonway.core.model.PaymentCard
import dev.liftonway.core.model.Rupees
import dev.liftonway.core.model.asPkr
import java.time.YearMonth

private val Presets = listOf(500L, 1000L, 2000L, 5000L)

/** Pick an amount and a card, pay, then coins drop into the wallet. */
@Composable
internal fun TopUpSheet(step: TopUpStep, cards: List<PaymentCard>, onPay: (Rupees, PaymentCard) -> Unit, onDismiss: () -> Unit, onAddCard: () -> Unit) {
    var preset by remember { mutableStateOf<Rupees?>(1000L) }
    var custom by remember { mutableStateOf("") }
    var cardId by remember { mutableStateOf<String?>(null) }
    // Falls back to the first card, including one added while this sheet is open.
    val card = cards.firstOrNull { it.id == cardId } ?: cards.firstOrNull()
    val amount = custom.toLongOrNull() ?: preset ?: 0L

    LiftSheet(onDismiss) { dismiss ->
        AnimatedContent(
            targetState = step is TopUpStep.Done,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "topUp",
        ) { done ->
            if (done) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    LiftLottie(LiftAnimation.WalletCoins, Modifier.size(180.dp))
                    Text("${(step as? TopUpStep.Done)?.amount?.asPkr().orEmpty()} added", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text("Your wallet has been topped up.", style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary)
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Done", dismiss, Modifier.fillMaxWidth())
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    Text("Top up", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Presets.forEach { value ->
                            Chip(value.asPkr().removePrefix("PKR "), selected = custom.isEmpty() && preset == value, Modifier.weight(1f)) {
                                preset = value
                                custom = ""
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LiftTextField(
                        custom,
                        { custom = it.filter(Char::isDigit).take(6) },
                        "Other amount (PKR)",
                        Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("PAY WITH", style = MaterialTheme.typography.labelSmall, color = LiftColors.TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    if (cards.isEmpty()) {
                        TextAction("+ADD CARD", onAddCard)
                    }
                    val selectedId = card?.id
                    cards.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(LiftShapes.Control)
                                .clickable(role = Role.RadioButton) { cardId = option.id }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Radio(option.id == selectedId)
                            Spacer(Modifier.width(14.dp))
                            BrandMark(option.brand)
                            Spacer(Modifier.width(14.dp))
                            Text("•••• ${option.last4}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        if (amount > 0) "Top up ${amount.asPkr()}" else "Top up",
                        { if (card != null) onPay(amount, card) },
                        Modifier.fillMaxWidth(),
                        enabled = amount > 0 && card != null,
                        loading = step == TopUpStep.Paying,
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) LiftColors.Green else LiftColors.Muted, label = "chip")
    val fg by animateColorAsState(if (selected) Color.White else LiftColors.Ink, label = "chipText")
    Box(
        modifier.height(40.dp).clip(LiftShapes.Control).background(bg).clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
}

@Composable
private fun Radio(selected: Boolean) {
    val ring by animateColorAsState(if (selected) LiftColors.Green else LiftColors.Hint, label = "radio")
    Box(Modifier.size(20.dp).border(1.6.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
        AnimatedContent(selected, transitionSpec = { scaleIn(Motion.bouncy()) togetherWith fadeOut() }, label = "dot") {
            if (it) Box(Modifier.size(10.dp).clip(CircleShape).background(LiftColors.Green))
        }
    }
}

/**
 * Card entry. Prototype only: the number lives in this sheet's state until Save, then only its last four
 * digits are kept. A real build hands these fields to the payment provider's SDK instead.
 */
@Composable
internal fun AddCardSheet(saving: Boolean, onSave: (String, () -> Unit) -> Unit, onDismiss: () -> Unit) {
    var number by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    val numberOk = number.length in 15..16 && luhn(number)
    val expiryOk = expiry.length == 4 && validExpiry(expiry)
    val cvvOk = cvv.length in 3..4
    val nameOk = name.isNotBlank()

    LiftSheet(onDismiss) { dismiss ->
        Text("Add card", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(18.dp))
        LiftTextField(
            number,
            { number = it.filter(Char::isDigit).take(16) },
            "Card number",
            Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Number,
            isError = showErrors && !numberOk,
            visualTransformation = Grouped(listOf(4, 8, 12), ' '),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LiftTextField(
                expiry,
                { expiry = it.filter(Char::isDigit).take(4) },
                "MM/YY",
                Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                isError = showErrors && !expiryOk,
                visualTransformation = Grouped(listOf(2), '/'),
            )
            LiftTextField(
                cvv,
                { cvv = it.filter(Char::isDigit).take(4) },
                "CVV",
                Modifier.weight(1f),
                keyboardType = KeyboardType.NumberPassword,
                isError = showErrors && !cvvOk,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        Spacer(Modifier.height(12.dp))
        LiftTextField(name, { name = it }, "Name on card", Modifier.fillMaxWidth(), imeAction = ImeAction.Done, isError = showErrors && !nameOk)
        Spacer(Modifier.height(10.dp))
        Text(
            "Prototype: use a test card number. Card details aren’t stored; only the last four digits are kept.",
            style = MaterialTheme.typography.bodySmall,
            color = LiftColors.Hint,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            "Save card",
            {
                showErrors = true
                if (numberOk && expiryOk && cvvOk && nameOk) {
                    val digits = number
                    number = ""
                    cvv = ""
                    onSave(digits, dismiss)
                }
            },
            Modifier.fillMaxWidth(),
            loading = saving,
        )
    }
}

/** Shows digits with a separator after the given positions, e.g. "4242 4242 ..." or "12/27". */
private class Grouped(private val breaks: List<Int>, private val separator: Char) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = buildString {
            text.text.forEachIndexed { i, c ->
                if (i in breaks) append(separator)
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + breaks.count { it < offset }
            override fun transformedToOriginal(offset: Int): Int {
                var original = offset
                breaks.forEachIndexed { i, b -> if (offset > b + i) original-- }
                return original.coerceIn(0, text.length)
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

private fun luhn(digits: String): Boolean = digits.reversed().mapIndexed { i, c ->
    val d = c.digitToInt()
    if (i % 2 == 1) (d * 2).let { if (it > 9) it - 9 else it } else d
}.sum() % 10 == 0

private fun validExpiry(mmyy: String): Boolean {
    val month = mmyy.take(2).toIntOrNull() ?: return false
    val year = mmyy.drop(2).toIntOrNull() ?: return false
    if (month !in 1..12) return false
    return !YearMonth.of(2000 + year, month).isBefore(YearMonth.now())
}
