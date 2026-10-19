package dev.liftonway.feature.trips

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.CancelResult
import dev.liftonway.core.data.RideRepository
import dev.liftonway.core.designsystem.component.Avatar
import dev.liftonway.core.designsystem.component.ConfirmSheetContent
import dev.liftonway.core.designsystem.component.EmptyState
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.MutedButton
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.RideCard
import dev.liftonway.core.designsystem.component.StatusBadge
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.Passenger
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.PublishedRide
import dev.liftonway.core.model.TripStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PublishedRideViewModel.Factory::class)
class PublishedRideViewModel @AssistedInject constructor(@Assisted private val rideId: String, private val rides: RideRepository) : ViewModel() {
    val ride: StateFlow<PublishedRide?> = rides.published
        .map { list -> list.firstOrNull { it.ride.id == rideId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), rides.published.value.firstOrNull { it.ride.id == rideId })

    /** Booking ids being answered, plus [CANCEL] or [COMPLETE] while those run. */
    val busy = mutableStateMapOf<String, Boolean>()

    /** The last failure, shown once in a snackbar. */
    var message by mutableStateOf<String?>(null)
    var confirmingCancel by mutableStateOf(false)
    var cancelFailure by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { attempt { rides.refreshPublished() } }
    }

    fun answer(passenger: Passenger, accept: Boolean) = run(passenger.bookingId) { rides.answer(passenger.bookingId, accept) }

    fun complete() = run(COMPLETE) { rides.completeRide(rideId) }

    fun askCancel() {
        cancelFailure = null
        confirmingCancel = true
    }

    fun cancel(onDone: () -> Unit) {
        if (busy[CANCEL] == true) return
        viewModelScope.launch {
            busy[CANCEL] = true
            try {
                when (rides.cancelRide(rideId)) {
                    CancelResult.Cancelled -> onDone()
                    CancelResult.TooLate -> cancelFailure = TOO_LATE
                }
            } catch (e: ApiError) {
                cancelFailure = e.message
            } finally {
                busy.remove(CANCEL)
            }
        }
    }

    private fun run(key: String, action: suspend () -> Unit) {
        if (busy[key] == true) return
        viewModelScope.launch {
            busy[key] = true
            attempt(action)
            busy.remove(key)
        }
    }

    private suspend fun attempt(action: suspend () -> Unit) {
        try {
            action()
        } catch (e: ApiError) {
            message = e.message
            // Someone may have withdrawn or the ride moved on; show what the server has now.
            runCatching { rides.refreshPublished() }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(rideId: String): PublishedRideViewModel
    }

    companion object {
        const val CANCEL = "cancel"
        const val COMPLETE = "complete"
        val TOO_LATE = "Rides with confirmed passengers can only be canceled before ${Policy.CancelCutoff.toHours()} hours."
    }
}

/** A ride the user is driving: answer requests, see who's coming, and cancel or complete it. */
@Composable
fun PublishedRideRoute(rideId: String, onBack: () -> Unit) {
    val viewModel = hiltViewModel<PublishedRideViewModel, PublishedRideViewModel.Factory>(key = "published-$rideId", creationCallback = { it.create(rideId) })
    val item by viewModel.ride.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.message = null
        }
    }

    Box(Modifier.fillMaxSize().background(LiftColors.Canvas).windowInsetsPadding(WindowInsets.navigationBars)) {
        Column(Modifier.fillMaxSize()) {
            LiftTopBar("Your ride", Modifier.background(Color.White), onBack = onBack)
            val ride = item
            if (ride == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("This ride isn't available any more", Modifier.padding(bottom = 120.dp), action = "Go back", onAction = onBack)
                }
            } else {
                RideDetails(ride, viewModel, Modifier.weight(1f))
                Actions(ride, viewModel)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (viewModel.confirmingCancel) {
        LiftSheet(onDismiss = { if (viewModel.busy[PublishedRideViewModel.CANCEL] != true) viewModel.confirmingCancel = false }) { dismiss ->
            val confirmed = item?.passengers.orEmpty().count { it.confirmed }
            ConfirmSheetContent(
                title = "Cancel this ride?",
                message = viewModel.cancelFailure ?: when (confirmed) {
                    0 -> "Anyone who asked for a seat will be told it's off."
                    1 -> "Your confirmed passenger will be told the ride is off."
                    else -> "Your $confirmed confirmed passengers will be told the ride is off."
                },
                primary = "Cancel ride",
                onPrimary = { viewModel.cancel(dismiss) },
                secondary = "Go Back",
                onSecondary = dismiss,
                primaryLoading = viewModel.busy[PublishedRideViewModel.CANCEL] == true,
            )
        }
    }
}

@Composable
private fun RideDetails(item: PublishedRide, viewModel: PublishedRideViewModel, modifier: Modifier) {
    val waiting = item.waiting
    val confirmed = item.passengers.filter { it.confirmed }
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "ride") {
            RideCard(item.ride, Modifier.entrance(0), dateTrailing = { StatusBadge(statusLabel(item), statusColor(item)) })
        }
        if (waiting.isNotEmpty()) {
            item(key = "requests") { SectionTitle("Requests", Modifier.animateItem()) }
            items(waiting, key = { it.bookingId }) { passenger ->
                PassengerRow(passenger, Modifier.animateItem()) {
                    if (item.status == TripStatus.Upcoming) {
                        val busy = viewModel.busy[passenger.bookingId] == true
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton("Accept", { viewModel.answer(passenger, true) }, Modifier.width(84.dp), height = 32.dp, loading = busy)
                            MutedButton("Decline", { viewModel.answer(passenger, false) }, height = 32.dp, enabled = !busy)
                        }
                    }
                }
            }
        }
        item(key = "passengers") { SectionTitle("Passengers", Modifier.animateItem()) }
        if (confirmed.isEmpty()) {
            item(key = "none") {
                Text(
                    if (item.status == TripStatus.Upcoming) "No one is confirmed yet. Requests show up here and in your notifications." else "No one rode with you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LiftColors.Hint,
                    modifier = Modifier.animateItem().fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
        } else {
            items(confirmed, key = { "c-" + it.bookingId }) { passenger ->
                PassengerRow(passenger, Modifier.animateItem()) { StatusBadge("Confirmed", LiftColors.Green) }
            }
        }
    }
}

@Composable
private fun Actions(item: PublishedRide, viewModel: PublishedRideViewModel) {
    AnimatedContent(Triple(item.status, item.canComplete, item.canCancel), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "actions") { (status, canComplete, canCancel) ->
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp)) {
            when {
                status != TripStatus.Upcoming -> Text(
                    if (status == TripStatus.Completed) "This ride is complete." else "You canceled this ride.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LiftColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                canComplete -> PrimaryButton(
                    "Complete ride",
                    viewModel::complete,
                    Modifier.fillMaxWidth(),
                    loading = viewModel.busy[PublishedRideViewModel.COMPLETE] == true,
                )
                else -> {
                    OutlineButton("Cancel ride", viewModel::askCancel, Modifier.fillMaxWidth(), enabled = canCancel)
                    if (!canCancel) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            PublishedRideViewModel.TOO_LATE,
                            style = MaterialTheme.typography.bodySmall,
                            color = LiftColors.Hint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = modifier.padding(start = 24.dp, top = 8.dp))
}

@Composable
private fun PassengerRow(passenger: Passenger, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(passenger.person.name, passenger.person.photoUri, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(passenger.person.name, style = MaterialTheme.typography.titleSmall)
            Text(if (passenger.seats == 1) "1 seat" else "${passenger.seats} seats", style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint)
        }
        trailing()
    }
}

internal fun statusLabel(item: PublishedRide) = when {
    item.status == TripStatus.Cancelled -> "Canceled"
    item.status == TripStatus.Completed -> "Completed"
    item.waiting.isNotEmpty() -> if (item.waiting.size == 1) "1 request" else "${item.waiting.size} requests"
    item.seatsBooked > 0 -> "${item.seatsBooked} booked"
    else -> "Open"
}

internal fun statusColor(item: PublishedRide) = when {
    item.status == TripStatus.Cancelled -> LiftColors.Hint
    item.status == TripStatus.Completed -> LiftColors.Blue
    item.waiting.isNotEmpty() -> LiftColors.Coral
    item.seatsBooked > 0 -> LiftColors.Blue
    else -> LiftColors.Green
}
