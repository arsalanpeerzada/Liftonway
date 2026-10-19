package dev.liftonway.feature.trips

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.liftonway.core.designsystem.component.ConfirmSheetContent
import dev.liftonway.core.designsystem.component.EmptyState
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.RatingSheet
import dev.liftonway.core.designsystem.component.RideCard
import dev.liftonway.core.designsystem.component.RideStats
import dev.liftonway.core.designsystem.component.Stars
import dev.liftonway.core.designsystem.component.StatusBadge
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.Booking
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.TripStatus
import dev.liftonway.core.model.asPkr
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared by the passenger's booking and the driver's ride: the rating sheet's state. */
class RatingState {
    var target by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var failure by mutableStateOf<String?>(null)
    var done by mutableStateOf(false)

    fun open(bookingId: String) {
        target = bookingId
        failure = null
        done = false
    }

    fun close() {
        if (!busy) target = null
    }
}

@HiltViewModel(assistedFactory = BookingDetailsViewModel.Factory::class)
class BookingDetailsViewModel @AssistedInject constructor(@Assisted private val bookingId: String, private val rides: RideRepository) : ViewModel() {
    val booking: StateFlow<Booking?> = rides.bookings
        .map { list -> list.firstOrNull { it.id == bookingId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), rides.bookings.value.firstOrNull { it.id == bookingId })

    var loading by mutableStateOf(booking.value == null)
        private set
    var confirmingCancel by mutableStateOf(false)
    var cancelBusy by mutableStateOf(false)
        private set
    var cancelFailure by mutableStateOf<String?>(null)
        private set
    val rating = RatingState()

    init {
        // Opened from a notification, the list may not be loaded yet.
        viewModelScope.launch {
            runCatching { rides.refreshBookings() }
            loading = false
        }
    }

    /** For display; the server makes the final call. */
    fun canCancel(booking: Booking) = booking.status == TripStatus.Upcoming && Policy.canCancel(booking.ride.departure, rides.now())

    fun askCancel() {
        cancelFailure = null
        confirmingCancel = true
    }

    fun cancel(onDone: () -> Unit) {
        if (cancelBusy) return
        viewModelScope.launch {
            cancelBusy = true
            try {
                when (rides.cancel(bookingId)) {
                    CancelResult.Cancelled -> onDone()
                    CancelResult.TooLate -> cancelFailure = "Ride can only be canceled before ${Policy.CancelCutoff.toHours()} hours."
                }
            } catch (e: ApiError) {
                cancelFailure = e.message
            } finally {
                cancelBusy = false
            }
        }
    }

    fun rate(stars: Int) = viewModelScope.launch { submitRating(rides, rating, stars) }

    @AssistedFactory
    interface Factory {
        fun create(bookingId: String): BookingDetailsViewModel
    }
}

internal suspend fun submitRating(rides: RideRepository, state: RatingState, stars: Int) {
    val target = state.target ?: return
    if (state.busy) return
    state.busy = true
    state.failure = null
    try {
        rides.rate(target, stars)
        state.done = true
    } catch (e: ApiError) {
        state.failure = e.message
    } finally {
        state.busy = false
    }
}

/** A seat the user asked for: the ride, the car, what it costs, and what they can do next. */
@Composable
fun BookingDetailsRoute(
    bookingId: String,
    onBack: () -> Unit,
    viewModel: BookingDetailsViewModel = hiltViewModel<BookingDetailsViewModel, BookingDetailsViewModel.Factory>(
        key = "booking-$bookingId",
        creationCallback = { it.create(bookingId) },
    ),
) {
    val item by viewModel.booking.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(LiftColors.Canvas).windowInsetsPadding(WindowInsets.navigationBars)) {
        LiftTopBar("Ride details", Modifier.background(Color.White), onBack = onBack)
        val booking = item
        when {
            booking != null -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RideCard(booking.ride, Modifier.entrance(0), dateTrailing = { StatusBadge(bookingLabel(booking), bookingColor(booking)) }) {
                        RideStats(booking.ride)
                    }
                    Panel("Your seat", Modifier.entrance(1)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (booking.seats == 1) "1 seat" else "${booking.seats} seats", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(booking.total.asPkr(), style = MaterialTheme.typography.titleSmall)
                        }
                        Text("Paid to the driver after the ride", style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint)
                    }
                    booking.vehicle?.let { car ->
                        Panel("Vehicle", Modifier.entrance(2)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LiftIcons.Car, null, tint = LiftColors.Hint, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(listOf(car.color, car.name, car.model).filter { it.isNotBlank() }.joinToString(" "), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        car.plate.ifBlank { "Number plate shown once the driver confirms" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (car.plate.isBlank()) LiftColors.Hint else LiftColors.TextSecondary,
                                    )
                                }
                            }
                            val extras = listOfNotNull("Air conditioning".takeIf { car.airConditioning }, "Heater".takeIf { car.heater })
                            if (extras.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { extras.forEach { StatusBadge(it, LiftColors.Blue) } }
                            }
                        }
                    }
                }
                BookingActions(booking, viewModel)
            }
            viewModel.loading -> Box(Modifier.fillMaxSize())
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("This ride isn't available any more", Modifier.padding(bottom = 120.dp), action = "Go back", onAction = onBack)
            }
        }
    }

    if (viewModel.confirmingCancel) {
        val allowed = item?.let(viewModel::canCancel) == true
        LiftSheet(onDismiss = { if (!viewModel.cancelBusy) viewModel.confirmingCancel = false }) { dismiss ->
            ConfirmSheetContent(
                title = "Are you sure you want to cancel?",
                message = viewModel.cancelFailure ?: if (allowed) {
                    "You can cancel until ${Policy.CancelCutoff.toHours()} hours before departure."
                } else {
                    "Ride can only be canceled before ${Policy.CancelCutoff.toHours()} hours."
                },
                primary = "Cancel",
                onPrimary = { viewModel.cancel(dismiss) },
                secondary = "Go Back",
                onSecondary = dismiss,
                primaryEnabled = allowed,
                primaryLoading = viewModel.cancelBusy,
            )
        }
    }

    val rating = viewModel.rating
    if (rating.target != null) {
        RatingSheet(
            title = "How was your ride with ${item?.ride?.driver?.name?.substringBefore(' ') ?: "your driver"}?",
            busy = rating.busy,
            failure = rating.failure,
            done = rating.done,
            onSubmit = viewModel::rate,
            onDismiss = rating::close,
        )
    }
}

@Composable
private fun BookingActions(booking: Booking, viewModel: BookingDetailsViewModel) {
    AnimatedContent(Triple(booking.status, booking.canRate, booking.myRating), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "actions") { (status, canRate, myRating) ->
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                status == TripStatus.Upcoming -> {
                    val allowed = viewModel.canCancel(booking)
                    OutlineButton(if (booking.confirmed) "Cancel seat" else "Cancel request", viewModel::askCancel, Modifier.fillMaxWidth(), enabled = allowed)
                    if (!allowed) {
                        Spacer(Modifier.height(8.dp))
                        Hint("Ride can only be canceled before ${Policy.CancelCutoff.toHours()} hours.")
                    }
                }
                canRate -> PrimaryButton("Rate ${booking.ride.driver.name.substringBefore(' ')}", { viewModel.rating.open(booking.id) }, Modifier.fillMaxWidth())
                myRating != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("You rated this ride", style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    Stars(myRating)
                }
                status == TripStatus.Completed -> Hint("This ride is complete.")
                else -> Hint(if (booking.confirmed) "This ride was canceled." else "This request is no longer active.")
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

internal fun bookingLabel(booking: Booking) = when (booking.status) {
    TripStatus.Upcoming -> if (booking.confirmed) "Confirmed" else "Pending"
    TripStatus.Completed -> "Completed"
    TripStatus.Cancelled -> "Canceled"
}

internal fun bookingColor(booking: Booking) = when (booking.status) {
    TripStatus.Upcoming -> if (booking.confirmed) LiftColors.Green else LiftColors.Coral
    TripStatus.Completed -> LiftColors.Blue
    TripStatus.Cancelled -> LiftColors.Hint
}
