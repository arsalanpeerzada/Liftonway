package dev.liftonway.feature.findride

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.IconAction
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.RideCard
import dev.liftonway.core.designsystem.component.RideStats
import dev.liftonway.core.designsystem.component.RouteStops
import dev.liftonway.core.designsystem.component.TitleAlign
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes
import dev.liftonway.core.model.Ride
import kotlinx.coroutines.launch

@Composable
fun AvailableRidesRoute(
    search: RideSearch,
    onBack: () -> Unit,
    onOpenStops: (String) -> Unit,
    onViewRequests: () -> Unit,
) {
    val viewModel = hiltViewModel<AvailableRidesViewModel, AvailableRidesViewModel.Factory>(
        key = search.toString(),
        creationCallback = { it.create(search) },
    )
    var sortSheet by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onRequest: (Ride) -> Unit = { ride ->
        viewModel.request(
            ride,
            onSent = {
                scope.launch {
                    val result = snackbar.showSnackbar("Request sent to ${it.driver.name.substringBefore(' ')}", actionLabel = "View")
                    if (result == SnackbarResult.ActionPerformed) onViewRequests()
                }
            },
            onFailed = { message -> scope.launch { snackbar.showSnackbar(message) } },
        )
    }

    Box(Modifier.fillMaxSize().background(LiftColors.Canvas)) {
        AnimatedContent(
            targetState = viewModel.state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(Motion.MEDIUM, delayMillis = 80)) togetherWith (fadeOut(tween(Motion.SHORT)) + scaleOut(targetScale = 0.96f)) },
            label = "rides",
        ) { state ->
            when (state) {
                RidesState.Searching -> Searching(onBack)
                is RidesState.Found -> Column(Modifier.fillMaxSize()) {
                    LiftTopBar(
                        "Available rides",
                        Modifier.background(Color.White),
                        onBack = onBack,
                        action = { IconAction(LiftIcons.Sliders, "Sort rides", { sortSheet = true }) },
                    )
                    val rides = viewModel.sorted(state.rides)
                    LazyColumn(
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        items(rides, key = { it.id }) { ride ->
                            RequestableRideCard(
                                ride,
                                viewModel.requests[ride.id] ?: RequestState.Idle,
                                { onOpenStops(ride.id) },
                                { onRequest(ride) },
                                Modifier.animateItem().entrance(rides.indexOf(ride).coerceAtMost(4)),
                            )
                        }
                    }
                }
                is RidesState.None -> NoRides(state.recommended, viewModel, onBack, onOpenStops, onRequest)
                is RidesState.Failed -> Column(Modifier.fillMaxSize().background(Color.White)) {
                    LiftTopBar("Available rides", onBack = onBack)
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton("Try again", viewModel::load, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp))
    }

    if (sortSheet) {
        LiftSheet({ sortSheet = false }) { dismiss ->
            Text("Sort rides", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            SortOrder.entries.forEach { order ->
                val selected = order == viewModel.sort
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(LiftShapes.Control)
                        .clickable(role = Role.RadioButton) {
                            viewModel.sort = order
                            dismiss()
                        }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(order.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (selected) Icon(LiftIcons.Check, null, tint = LiftColors.Green)
                }
            }
        }
    }
}

@Composable
private fun Searching(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        LiftTopBar("Available rides", onBack = onBack)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            LiftLottie(LiftAnimation.Searching, Modifier.size(200.dp))
            Text("Finding rides on your route…", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun RequestableRideCard(ride: Ride, request: RequestState, onOpen: () -> Unit, onRequest: () -> Unit, modifier: Modifier = Modifier) {
    RideCard(ride, modifier, onOpen = onOpen) {
        RideStats(ride)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(request == RequestState.Sent, label = "request") { sent ->
            if (sent) {
                OutlineButton("Requested", {}, Modifier.fillMaxWidth(), enabled = false)
            } else {
                PrimaryButton("Request", onRequest, Modifier.fillMaxWidth(), loading = request == RequestState.Sending)
            }
        }
    }
}

/** "No rides available", with the same route on the following days in a sideways list. */
@Composable
private fun NoRides(
    recommended: List<Ride>,
    viewModel: AvailableRidesViewModel,
    onBack: () -> Unit,
    onOpenStops: (String) -> Unit,
    onRequest: (Ride) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Color.White), horizontalAlignment = Alignment.CenterHorizontally) {
            LiftTopBar("", onBack = onBack)
            LiftLottie(LiftAnimation.NoRides, Modifier.size(width = 190.dp, height = 130.dp))
            Text("No rides available", style = MaterialTheme.typography.titleMedium, modifier = Modifier.entrance(1))
            Spacer(Modifier.height(10.dp))
            Text(
                "There are no rides available in your area at the moment. But don’t worry, you can ride for next date or choose from recommended rides below.",
                style = MaterialTheme.typography.bodySmall,
                color = LiftColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 56.dp).entrance(2),
            )
            Spacer(Modifier.height(36.dp))
        }
        Text("Recommended rides", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recommended, key = { it.id }) { ride ->
                RequestableRideCard(
                    ride,
                    viewModel.requests[ride.id] ?: RequestState.Idle,
                    { onOpenStops(ride.id) },
                    { onRequest(ride) },
                    Modifier.width(310.dp).clip(LiftShapes.Card).entrance(recommended.indexOf(ride) + 3),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** "Available stops": the ride's route, top to bottom. */
@Composable
fun RideStopsRoute(rideId: String, onBack: () -> Unit) {
    val viewModel = hiltViewModel<RideStopsViewModel, RideStopsViewModel.Factory>(key = "stops-$rideId", creationCallback = { it.create(rideId) })
    Column(Modifier.fillMaxSize().background(LiftColors.Canvas)) {
        LiftTopBar("Available stops", Modifier.background(Color.White), onBack = onBack, align = TitleAlign.Center)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().weight(1f).background(Color.White).padding(horizontal = 24.dp, vertical = 24.dp)) {
            val ride = viewModel.ride
            if (ride != null) {
                RouteStops(ride.allStops.map { it.label }, Modifier.entrance(0), emphasiseLast = true)
                if (ride.route.isNotBlank()) {
                    Spacer(Modifier.height(24.dp))
                    Text("ROUTE", style = MaterialTheme.typography.labelSmall, color = LiftColors.TextSecondary)
                    Text(ride.route, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(80.dp).windowInsetsPadding(WindowInsets.navigationBars))
    }
}
