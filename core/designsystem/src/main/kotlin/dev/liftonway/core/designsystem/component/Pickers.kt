package dev.liftonway.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.Jakarta
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.Policy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** A sheet with a big seat count; the number rolls up or down as it changes. */
@Composable
fun SeatsSheet(title: String, seats: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    LiftSheet(onDismiss) { dismiss ->
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            StepButton(LiftIcons.Close, "Fewer seats", enabled = seats > 1, minus = true) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onChange(seats - 1)
            }
            Spacer(Modifier.width(36.dp))
            AnimatedContent(
                targetState = seats,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { if (up) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { if (up) -it else it } + fadeOut())
                },
                label = "seats",
            ) {
                Text(it.toString(), style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Light, fontSize = 72.sp, color = LiftColors.Ink))
            }
            Spacer(Modifier.width(36.dp))
            StepButton(LiftIcons.Plus, "More seats", enabled = seats < Policy.MAX_SEATS) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onChange(seats + 1)
            }
        }
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Done", dismiss, Modifier.fillMaxWidth())
    }
}

/**
 * The fare per seat: steps of [Policy.FARE_STEP] either way, or one of the common fares in a tap.
 * The amount rolls like the seat count.
 */
@Composable
fun PriceSheet(price: Long, onChange: (Long) -> Unit, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    fun set(value: Long) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onChange(value.coerceIn(Policy.MIN_FARE, Policy.MAX_FARE))
    }
    LiftSheet(onDismiss) { dismiss ->
        Text("Price per seat", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            StepButton(LiftIcons.Close, "Lower price", enabled = price > Policy.MIN_FARE, minus = true) { set(price - Policy.FARE_STEP) }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.width(170.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PKR", style = MaterialTheme.typography.bodyMedium, color = LiftColors.Hint)
                AnimatedContent(
                    targetState = price,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { if (up) it else -it } + fadeIn()) togetherWith
                            (slideOutVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { if (up) -it else it } + fadeOut())
                    },
                    label = "price",
                ) {
                    Text("%,d".format(it), style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Light, fontSize = 56.sp, color = LiftColors.Ink))
                }
            }
            Spacer(Modifier.width(20.dp))
            StepButton(LiftIcons.Plus, "Higher price", enabled = price < Policy.MAX_FARE) { set(price + Policy.FARE_STEP) }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(300L, 500L, 800L, 1200L, 2000L).forEach { fare ->
                val selected = fare == price
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (selected) LiftColors.Green else LiftColors.Muted)
                        .clickable(role = Role.RadioButton, onClickLabel = "PKR $fare") { set(fare) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("%,d".format(fare), style = MaterialTheme.typography.bodyMedium, color = if (selected) Color.White else LiftColors.TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Done", dismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, minus: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) LiftColors.Ink else LiftColors.Muted)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (minus) {
            Box(Modifier.width(16.dp).height(2.5.dp).clip(CircleShape).background(Color.White).semantics { contentDescription = label })
        } else {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

/** Material's date picker in the app's colours; only today and later can be picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftDatePicker(date: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            override fun isSelectableYear(year: Int) = year >= today.year
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = Color.White,
        selectedDayContainerColor = LiftColors.Green,
        todayDateBorderColor = LiftColors.Green,
        todayContentColor = LiftColors.Green,
        selectedYearContainerColor = LiftColors.Green,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = LiftColors.Green) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = LiftColors.TextSecondary) } },
        colors = colors,
    ) {
        DatePicker(state, colors = colors, title = { Text("When are you going?", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

/** Material's time picker in a sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSheet(time: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(time.hour, time.minute, is24Hour = false)
    LiftSheet(onDismiss) { dismiss ->
        Text("When are you leaving?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialSelectedContentColor = Color.White,
                    selectorColor = LiftColors.Green,
                    periodSelectorSelectedContainerColor = LiftColors.GreenSoft,
                    timeSelectorSelectedContainerColor = LiftColors.GreenSoft,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Done", {
            onPick(LocalTime.of(state.hour, state.minute))
            dismiss()
        }, Modifier.fillMaxWidth())
    }
}
