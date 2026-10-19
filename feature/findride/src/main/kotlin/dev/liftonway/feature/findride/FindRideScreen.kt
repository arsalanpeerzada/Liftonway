package dev.liftonway.feature.findride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.liftonway.core.designsystem.component.LiftDatePicker
import dev.liftonway.core.designsystem.component.LiftTextField
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.PlaceSuggestions
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.SearchGlyph
import dev.liftonway.core.designsystem.component.SeatsSheet
import dev.liftonway.core.designsystem.component.SelectorRow
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.chip

/** The search form: pickup, destination, date and seats. */
@Composable
fun FindRideRoute(onClose: () -> Unit, onSearch: (RideSearch) -> Unit, viewModel: FindRideViewModel = hiltViewModel()) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var seatsSheet by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val destinationFocus = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().background(LiftColors.Canvas).imePadding()) {
        LiftTopBar("Find a ride", Modifier.background(Color.White), onBack = onClose, navIcon = LiftIcons.Close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 32.dp, vertical = 22.dp)) {
                LocationInput(
                    viewModel,
                    LocationField.Pickup,
                    "Search Pickup",
                    isError = viewModel.pickupError,
                    modifier = Modifier.entrance(0),
                    onChosen = { destinationFocus.requestFocus() },
                )
                Spacer(Modifier.height(12.dp))
                LocationInput(
                    viewModel,
                    LocationField.Destination,
                    "Search destination",
                    isError = viewModel.destinationError,
                    modifier = Modifier.entrance(1).focusRequester(destinationFocus),
                    onChosen = { focus.clearFocus() },
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().background(Color.White).height(84.dp).padding(horizontal = 28.dp, vertical = 14.dp).entrance(2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectorRow(LiftIcons.Calendar, viewModel.date.chip(), { datePicker = true }, Modifier.weight(1f), "Change date")
                VerticalDivider(Modifier.padding(horizontal = 12.dp).height(34.dp), color = LiftColors.Divider)
                SelectorRow(LiftIcons.Person, viewModel.seats.toString(), { seatsSheet = true }, Modifier.weight(0.6f), "Change seats")
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                "Search a ride",
                {
                    focus.clearFocus()
                    viewModel.search(onSearch)
                },
                Modifier.fillMaxWidth().padding(horizontal = 32.dp).entrance(3),
            )
        }
    }

    if (datePicker) LiftDatePicker(viewModel.date, { viewModel.date = it }, { datePicker = false })
    if (seatsSheet) SeatsSheet("How many seats are you looking for?", viewModel.seats, { viewModel.changeSeats(it - viewModel.seats) }, { seatsSheet = false })
}

/** A location field whose suggestions unfold beneath it while it has focus. */
@Composable
private fun LocationInput(
    viewModel: FindRideViewModel,
    field: LocationField,
    placeholder: String,
    isError: Boolean,
    modifier: Modifier,
    onChosen: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val chosen = if (field == LocationField.Pickup) viewModel.pickup else viewModel.destination
    LaunchedEffect(focused) { if (focused) viewModel.lookUp(field) }
    Column {
        LiftTextField(
            viewModel.text(field),
            { viewModel.onText(field, it) },
            placeholder,
            modifier.fillMaxWidth(),
            leading = { SearchGlyph() },
            isError = isError,
            interactionSource = interaction,
        )
        AnimatedVisibility(focused && chosen == null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            PlaceSuggestions(viewModel.suggestions(field)) {
                viewModel.choose(field, it)
                onChosen()
            }
        }
    }
}
