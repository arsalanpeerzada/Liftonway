package dev.liftonway.feature.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.liftonway.core.designsystem.component.ConfirmSheetContent
import dev.liftonway.core.designsystem.component.EmptyState
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.RideCard
import dev.liftonway.core.designsystem.component.SegmentedTabs
import dev.liftonway.core.designsystem.component.StatusBadge
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.Policy
import dev.liftonway.core.model.TripStatus
import kotlinx.coroutines.launch

private val Tabs = listOf(TripStatus.Upcoming, TripStatus.Completed, TripStatus.Cancelled)
private val TabLabels = listOf("Upcoming", "Completed", "Canceled")

/** The My trips tab: a choice between rides you asked for and rides you posted. */
@Composable
fun MyTripsRoute(onBack: () -> Unit, onRequested: () -> Unit, onPublished: () -> Unit, contentPadding: PaddingValues) {
    Column(Modifier.fillMaxSize().background(Color.White).padding(contentPadding)) {
        LiftTopBar("", onBack = onBack)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val pill = RoundedCornerShape(50)
            PrimaryButton("Requested rides", onRequested, Modifier.width(182.dp).entrance(0), shape = pill, height = 54.dp)
            Spacer(Modifier.height(22.dp))
            OutlineButton("Published rides", onPublished, Modifier.width(182.dp).entrance(1), shape = pill, height = 54.dp)
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun RequestedRidesRoute(onBack: () -> Unit, onBookRide: () -> Unit, viewModel: TripsViewModel = hiltViewModel()) {
    val groups by viewModel.bookings.collectAsStateWithLifecycle()
    TabbedTrips("Requested rides", onBack) { status ->
        val list = groups[status].orEmpty()
        if (list.isEmpty()) {
            Empty("You have not ${emptyVerb(status)} a ride yet!!", "Book a ride", onBookRide)
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(list, key = { _, it -> it.id }) { i, booking ->
                    val upcoming = status == TripStatus.Upcoming
                    RideCard(
                        booking.ride,
                        Modifier
                            .animateItem()
                            .entrance(i.coerceAtMost(4))
                            .then(if (upcoming) Modifier.clickable(role = Role.Button, onClickLabel = "Manage request") { viewModel.askCancel(booking) } else Modifier),
                        dateTrailing = if (upcoming) {
                            { StatusBadge(if (booking.confirmed) "Confirmed" else "Pending", if (booking.confirmed) LiftColors.Green else LiftColors.Coral) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    val booking = viewModel.cancelling
    if (booking != null) {
        val allowed = viewModel.canCancel(booking)
        LiftSheet(onDismiss = viewModel::dismissCancel) { dismiss ->
            ConfirmSheetContent(
                title = "Are you sure you want to cancel?",
                message = if (allowed) {
                    "You can cancel until ${Policy.CancelCutoff.toHours()} hours before departure."
                } else {
                    "Ride can only be canceled before ${Policy.CancelCutoff.toHours()} hours."
                },
                primary = "Cancel",
                onPrimary = { viewModel.confirmCancel(dismiss) },
                secondary = "Go Back",
                onSecondary = dismiss,
                primaryEnabled = allowed,
                primaryLoading = viewModel.cancelBusy,
            )
        }
    }
}

@Composable
fun PublishedRidesRoute(onBack: () -> Unit, onPublish: () -> Unit, viewModel: TripsViewModel = hiltViewModel()) {
    val groups by viewModel.published.collectAsStateWithLifecycle()
    TabbedTrips("Published rides", onBack) { status ->
        val list = groups[status].orEmpty()
        if (list.isEmpty()) {
            Empty("You have not ${if (status == TripStatus.Upcoming) "published" else emptyVerb(status)} a ride yet!!", "Publish a ride", onPublish)
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(list, key = { _, it -> it.ride.id }) { i, item ->
                    RideCard(
                        item.ride,
                        Modifier.animateItem().entrance(i.coerceAtMost(4)),
                        dateTrailing = {
                            val booked = item.passengers.size
                            StatusBadge(if (booked == 0) "Open" else "$booked booked", if (booked == 0) LiftColors.Green else LiftColors.Blue)
                        },
                    )
                }
            }
        }
    }
}

private fun emptyVerb(status: TripStatus) = when (status) {
    TripStatus.Upcoming -> "requested"
    TripStatus.Completed -> "completed"
    TripStatus.Cancelled -> "canceled"
}

/** Title, the sliding tabs and a pager underneath them; swiping and tapping both move the pill. */
@Composable
private fun TabbedTrips(title: String, onBack: () -> Unit, page: @Composable (TripStatus) -> Unit) {
    val pager = rememberPagerState { Tabs.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(LiftColors.Canvas).windowInsetsPadding(WindowInsets.navigationBars)) {
        LiftTopBar(title, Modifier.background(Color.White), onBack = onBack)
        Spacer(Modifier.height(4.dp))
        SegmentedTabs(TabLabels, { pager.currentPage + pager.currentPageOffsetFraction }, { scope.launch { pager.animateScrollToPage(it) } })
        Spacer(Modifier.height(6.dp))
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { Tabs[it] }) { index ->
            Box(Modifier.fillMaxSize()) { page(Tabs[index]) }
        }
    }
}

@Composable
private fun Empty(message: String, action: String, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(message, Modifier.padding(bottom = 120.dp), action = action, onAction = onAction)
    }
}
