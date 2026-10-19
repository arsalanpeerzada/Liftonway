package dev.liftonway.feature.publish

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.IconAction
import dev.liftonway.core.designsystem.component.LiftDatePicker
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTextField
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PlaceSuggestions
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.SearchGlyph
import dev.liftonway.core.designsystem.component.SeatsSheet
import dev.liftonway.core.designsystem.component.Section
import dev.liftonway.core.designsystem.component.SelectorRow
import dev.liftonway.core.designsystem.component.PriceSheet
import dev.liftonway.core.designsystem.component.TextAction
import dev.liftonway.core.designsystem.component.TimeSheet
import dev.liftonway.core.designsystem.component.shakeOnError
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.asPkr
import dev.liftonway.core.model.chip
import dev.liftonway.core.model.clock

@Composable
fun PublishRideRoute(
    onClose: () -> Unit,
    onAddVehicle: () -> Unit,
    onViewPublished: () -> Unit,
    viewModel: PublishViewModel = hiltViewModel(),
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timeSheet by rememberSaveable { mutableStateOf(false) }
    var seatsSheet by rememberSaveable { mutableStateOf(false) }
    var priceSheet by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().background(LiftColors.Canvas).imePadding()) {
        LiftTopBar("Publish a ride", Modifier.background(Color.White), onBack = onClose, navIcon = LiftIcons.Close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Spacer(Modifier.height(4.dp))
            Section(Modifier.animateContentSize().entrance(0)) {
                AddressInput(viewModel, viewModel.pickup, "Pickup Address")
                Spacer(Modifier.height(12.dp))
                viewModel.stops.forEachIndexed { i, stop ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { AddressInput(viewModel, stop, "Stop ${i + 1} address") }
                        IconAction(LiftIcons.Close, "Remove stop", { viewModel.removeStop(stop) }, tint = LiftColors.TextSecondary)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                AddressInput(viewModel, viewModel.destination, "Destination address")
                if (viewModel.stops.size < PublishViewModel.MAX_STOPS) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        TextAction("+Add stop", viewModel::addStop)
                    }
                }
            }
            Section(Modifier.height(96.dp).entrance(1), label = "Departure date") {
                SelectorRow(LiftIcons.Calendar, viewModel.date.chip(), { datePicker = true }, contentDescription = "Change departure date")
            }
            Row(Modifier.fillMaxWidth().height(96.dp).entrance(2), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Section(Modifier.weight(1f), label = "Select time") {
                    SelectorRow(LiftIcons.Clock, viewModel.time.clock(), { timeSheet = true }, contentDescription = "Change departure time")
                }
                Section(Modifier.weight(1f), label = "Available seats") {
                    SelectorRow(LiftIcons.Person, viewModel.seats.toString(), { seatsSheet = true }, contentDescription = "Change seats")
                }
            }
            Section(Modifier.height(96.dp).entrance(3), label = "Price per seat") {
                SelectorRow(LiftIcons.Banknote, viewModel.price.asPkr(), { priceSheet = true }, contentDescription = "Change price per seat")
            }
            Section(Modifier.entrance(3), label = "Add route") {
                LiftTextField(viewModel.route, { viewModel.route = it }, "Enter your route here", Modifier.fillMaxWidth(), onImeAction = { focus.clearFocus() })
            }
            Section(Modifier.shakeOnError(viewModel.missingVehicle).entrance(4), label = "Vehicle") {
                AnimatedContent(vehicle, label = "vehicle") { car ->
                    if (car == null) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            TextAction("+Add Vehicle", onAddVehicle, color = if (viewModel.missingVehicle) LiftColors.Coral else LiftColors.Green)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LiftIcons.Car, null, tint = LiftColors.Hint, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(car.summary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                "Continue",
                {
                    focus.clearFocus()
                    viewModel.publish()
                },
                Modifier.fillMaxWidth().padding(horizontal = 28.dp).entrance(5),
                loading = viewModel.publishing,
            )
            AnimatedVisibility(viewModel.failure != null) {
                Text(
                    viewModel.failure.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = LiftColors.Coral,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                )
            }
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
    }

    if (datePicker) LiftDatePicker(viewModel.date, { viewModel.date = it }, { datePicker = false })
    if (timeSheet) TimeSheet(viewModel.time, { viewModel.time = it }, { timeSheet = false })
    if (priceSheet) PriceSheet(viewModel.price, { viewModel.price = it }, { priceSheet = false })
    if (seatsSheet) SeatsSheet("How many seats are you offering?", viewModel.seats, { viewModel.changeSeats(it - viewModel.seats) }, { seatsSheet = false })
    if (viewModel.published) {
        LiftSheet(onDismiss = onClose) { dismiss ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LiftLottie(LiftAnimation.RouteMap, Modifier.size(width = 220.dp, height = 180.dp)) }
            Text("Ride published!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(
                "We’ll let you know when someone requests a seat.",
                style = MaterialTheme.typography.bodyMedium,
                color = LiftColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlineButton("Done", dismiss, Modifier.weight(1f))
                PrimaryButton("My rides", onViewPublished, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AddressInput(viewModel: PublishViewModel, field: AddressField, placeholder: String) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) viewModel.lookUp(field) }
    Column {
        LiftTextField(
            field.text,
            { viewModel.onText(field, it) },
            placeholder,
            Modifier.fillMaxWidth(),
            leading = { SearchGlyph() },
            isError = viewModel.error(field),
            interactionSource = interaction,
        )
        AnimatedVisibility(focused && field.place == null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            PlaceSuggestions(field.suggestions) { viewModel.choose(field, it) }
        }
    }
}
