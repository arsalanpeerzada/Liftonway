package dev.liftonway.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import dev.liftonway.core.designsystem.component.BarItem
import dev.liftonway.core.designsystem.component.LiftBottomBar
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.LocalNavAnimatedScope
import dev.liftonway.core.designsystem.motion.LocalSharedTransitionScope
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.feature.auth.AuthChoiceRoute
import dev.liftonway.feature.auth.OtpRoute
import dev.liftonway.feature.auth.SignInRoute
import dev.liftonway.feature.auth.SignUpRoute
import dev.liftonway.feature.auth.VerifiedRoute
import dev.liftonway.feature.findride.AvailableRidesRoute
import dev.liftonway.feature.findride.FindRideRoute
import dev.liftonway.feature.findride.RideSearch
import dev.liftonway.feature.findride.RideStopsRoute
import dev.liftonway.feature.home.HomeRoute
import dev.liftonway.feature.notifications.NotificationsRoute
import dev.liftonway.feature.onboarding.SplashRoute
import dev.liftonway.feature.onboarding.TermsRoute
import dev.liftonway.feature.onboarding.WalkthroughRoute
import dev.liftonway.feature.profile.ChangeNumberRoute
import dev.liftonway.feature.profile.EditProfileRoute
import dev.liftonway.feature.profile.InfoRoute
import dev.liftonway.feature.profile.ProfileDestination
import dev.liftonway.feature.profile.ProfileRoute
import dev.liftonway.feature.profile.VehicleRoute
import dev.liftonway.feature.publish.PublishRideRoute
import dev.liftonway.feature.trips.MyTripsRoute
import dev.liftonway.feature.trips.BookingDetailsRoute
import dev.liftonway.feature.trips.PublishedRideRoute
import dev.liftonway.feature.trips.PublishedRidesRoute
import dev.liftonway.feature.trips.RequestedRidesRoute
import dev.liftonway.feature.wallet.TransactionHistoryRoute
import dev.liftonway.feature.wallet.WalletRoute

sealed interface Destination
data object WalkthroughKey : Destination
data object TermsKey : Destination
data object AuthChoiceKey : Destination
data object SignUpKey : Destination
data object SignInKey : Destination
data object OtpKey : Destination
data class VerifiedKey(val thenMain: Boolean) : Destination
data object MainKey : Destination
data object FindRideKey : Destination
data class AvailableRidesKey(val search: RideSearch) : Destination
data class StopsKey(val rideId: String) : Destination
data object PublishKey : Destination
data object RequestedKey : Destination
data object PublishedKey : Destination
data class PublishedRideKey(val rideId: String) : Destination
data class BookingKey(val bookingId: String) : Destination
data object EditProfileKey : Destination
data object ChangeNumberKey : Destination
data object VehicleKey : Destination
data object WalletKey : Destination
data object HistoryKey : Destination
data class InfoKey(val destination: ProfileDestination) : Destination

private enum class Tab { Home, Trips, Notifications, Profile }

/**
 * The app shell on Navigation 3: we own the back stack. Screens slide in over the one beneath, which drifts
 * a little the other way (parallax); predictive back previews the same motion. The splash sits on top until
 * the session is known and its animation has played, then fades away to reveal the first screen.
 */
@Composable
fun LiftOnWayApp(app: AppViewModel = hiltViewModel()) {
    val session by app.session.collectAsStateWithLifecycle()
    val pending by app.pending.collectAsStateWithLifecycle()
    val backStack = remember { mutableStateListOf<Destination>() }
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(Tab.Home.ordinal) }

    fun reset(vararg keys: Destination) {
        backStack.clear()
        backStack.addAll(keys)
    }
    fun push(key: Destination) = backStack.add(key)
    fun pop() = backStack.removeLastOrNull()

    // Choose the first screen once DataStore has answered.
    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        if (backStack.isEmpty()) {
            when {
                !s.onboarded -> reset(WalkthroughKey)
                !s.signedIn -> reset(AuthChoiceKey)
                else -> reset(MainKey)
            }
        }
    }

    // The server stopped accepting the token: go back to sign-in rather than failing every call.
    LaunchedEffect(session?.signedIn) {
        if (session?.signedIn == false && backStack.contains(MainKey)) reset(AuthChoiceKey)
    }

    // New requests and answers arrive while the app is open; nothing runs once it's in the background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(session?.signedIn) {
        if (session?.signedIn == true) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { app.keepNotificationsFresh() }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        if (backStack.isNotEmpty()) {
            SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { pop() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        sharedTransitionScope = this,
                        transitionSpec = { forwardTransition() },
                        popTransitionSpec = { backTransition() },
                        predictivePopTransitionSpec = { backTransition() },
                        entryProvider = entryProvider {
                            entry<WalkthroughKey> { Animated { WalkthroughRoute(onDone = { push(TermsKey) }) } }
                            entry<TermsKey> {
                                Animated { TermsRoute(onStart = { reset(AuthChoiceKey) }, onOpenTerms = { push(InfoKey(ProfileDestination.Terms)) }) }
                            }
                            entry<AuthChoiceKey> { Animated { AuthChoiceRoute(onSignUp = { push(SignUpKey) }, onSignIn = { push(SignInKey) }) } }
                            entry<SignUpKey> { Animated { SignUpRoute(onBack = { pop() }, onCodeSent = { push(OtpKey) }) } }
                            entry<SignInKey> { Animated { SignInRoute(onBack = { pop() }, onCodeSent = { push(OtpKey) }) } }
                            entry<OtpKey> {
                                Animated {
                                    OtpRoute(
                                        onBack = { pop() },
                                        onVerified = {
                                            val signedIn = session?.signedIn == true && backStack.contains(MainKey)
                                            if (signedIn) {
                                                // Changing number: back to the profile through the success screen.
                                                backStack.removeAll { it == OtpKey || it == ChangeNumberKey }
                                                push(VerifiedKey(thenMain = false))
                                            } else {
                                                tab = Tab.Home.ordinal
                                                reset(VerifiedKey(thenMain = true))
                                            }
                                        },
                                    )
                                }
                            }
                            entry<VerifiedKey> { key -> Animated { VerifiedRoute(onContinue = { if (key.thenMain) reset(MainKey) else pop() }) } }
                            entry<MainKey> {
                                Animated {
                                    MainTabs(
                                        tab = Tab.entries[tab],
                                        pendingRequests = pending,
                                        onTab = { tab = it.ordinal },
                                        navigate = ::push,
                                        onSignedOut = { reset(AuthChoiceKey) },
                                    )
                                }
                            }
                            entry<FindRideKey> { Animated { FindRideRoute(onClose = { pop() }, onSearch = { push(AvailableRidesKey(it)) }) } }
                            entry<AvailableRidesKey> { key ->
                                Animated {
                                    AvailableRidesRoute(
                                        key.search,
                                        onBack = { pop() },
                                        onOpenStops = { push(StopsKey(it)) },
                                        onViewRequests = {
                                            tab = Tab.Trips.ordinal
                                            reset(MainKey, RequestedKey)
                                        },
                                    )
                                }
                            }
                            entry<StopsKey> { key -> Animated { RideStopsRoute(key.rideId, onBack = { pop() }) } }
                            entry<PublishKey> {
                                Animated {
                                    PublishRideRoute(
                                        onClose = { pop() },
                                        onAddVehicle = { push(VehicleKey) },
                                        onViewPublished = {
                                            tab = Tab.Trips.ordinal
                                            reset(MainKey, PublishedKey)
                                        },
                                    )
                                }
                            }
                            entry<RequestedKey> {
                                Animated { RequestedRidesRoute(onBack = { pop() }, onBookRide = { backStack[backStack.lastIndex] = FindRideKey }, onOpen = { push(BookingKey(it)) }) }
                            }
                            entry<PublishedKey> {
                                Animated {
                                    PublishedRidesRoute(
                                        onBack = { pop() },
                                        onPublish = { backStack[backStack.lastIndex] = PublishKey },
                                        onOpen = { push(PublishedRideKey(it)) },
                                    )
                                }
                            }
                            entry<PublishedRideKey> { key -> Animated { PublishedRideRoute(key.rideId, onBack = { pop() }) } }
                            entry<BookingKey> { key -> Animated { BookingDetailsRoute(key.bookingId, onBack = { pop() }) } }
                            entry<EditProfileKey> { Animated { EditProfileRoute(onBack = { pop() }) } }
                            entry<ChangeNumberKey> { Animated { ChangeNumberRoute(onBack = { pop() }, onCodeSent = { push(OtpKey) }) } }
                            entry<VehicleKey> { Animated { VehicleRoute(onBack = { pop() }) } }
                            entry<WalletKey> { Animated { WalletRoute(onBack = { pop() }, onHistory = { push(HistoryKey) }) } }
                            entry<HistoryKey> { Animated { TransactionHistoryRoute(onBack = { pop() }) } }
                            entry<InfoKey> { key -> Animated { InfoRoute(key.destination, onBack = { pop() }) } }
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !splashDone || session == null,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(Motion.LONG)) + scaleOut(tween(Motion.LONG), targetScale = 1.08f),
        ) {
            SplashRoute(onFinished = { splashDone = true })
        }
    }
}

/** Forward: the new screen slides in from the right while the old one drifts left and dims slightly. */
private fun forwardTransition(): ContentTransform =
    slideInHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { it } togetherWith
        (slideOutHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { -it / 4 } + fadeOut(tween(Motion.LONG), targetAlpha = 0.6f))

/** Back: the reverse, with the leaving screen kept on top as it slides away. */
private fun backTransition(): ContentTransform =
    (
        (slideInHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { -it / 4 } + fadeIn(tween(Motion.LONG), initialAlpha = 0.6f)) togetherWith
            slideOutHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { it }
        ).apply { targetContentZIndex = -1f }

/** Hands this destination's enter and exit animation to shared elements further down. */
@Composable
private fun Animated(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides LocalNavAnimatedContentScope.current, content = content)
}

/** The four tabs. Switching fades through, and each tab keeps its scroll position. */
@Composable
private fun MainTabs(
    tab: Tab,
    pendingRequests: Int,
    onTab: (Tab) -> Unit,
    navigate: (Destination) -> Unit,
    onSignedOut: () -> Unit,
) {
    BackHandler(enabled = tab != Tab.Home) { onTab(Tab.Home) }
    val states = rememberSaveableStateHolder()
    val items = listOf(
        BarItem("Home", LiftIcons.Home),
        BarItem("My trips", LiftIcons.Trips),
        BarItem("Notifications", LiftIcons.Bell, badge = pendingRequests),
        BarItem("Profile", LiftIcons.Profile),
    )
    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { LiftBottomBar(items, tab.ordinal, { onTab(Tab.entries[it]) }) },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(Motion.MEDIUM, delayMillis = 60)) + scaleIn(tween(Motion.MEDIUM, delayMillis = 60), initialScale = 0.97f)) togetherWith
                    fadeOut(tween(Motion.SHORT))
            },
            label = "tabs",
        ) { current ->
            states.SaveableStateProvider(current.name) {
                TabContent(current, padding, onTab, navigate, onSignedOut)
            }
        }
    }
}

@Composable
private fun TabContent(tab: Tab, padding: PaddingValues, onTab: (Tab) -> Unit, navigate: (Destination) -> Unit, onSignedOut: () -> Unit) {
    val home = { onTab(Tab.Home) }
    when (tab) {
        Tab.Home -> HomeRoute(onFindRide = { navigate(FindRideKey) }, onPublishRide = { navigate(PublishKey) }, contentPadding = padding)
        Tab.Trips -> MyTripsRoute(onBack = home, onRequested = { navigate(RequestedKey) }, onPublished = { navigate(PublishedKey) }, contentPadding = padding)
        Tab.Notifications -> NotificationsRoute(
            onBack = home,
            onOpenWallet = { navigate(WalletKey) },
            onOpenRide = { navigate(PublishedRideKey(it)) },
            onOpenBooking = { navigate(BookingKey(it)) },
            contentPadding = padding,
        )
        Tab.Profile -> ProfileRoute(
            onBack = home,
            onEditProfile = { navigate(EditProfileKey) },
            onChangeNumber = { navigate(ChangeNumberKey) },
            onWallet = { navigate(WalletKey) },
            onPaymentHistory = { navigate(HistoryKey) },
            onVehicle = { navigate(VehicleKey) },
            onInfo = { navigate(InfoKey(it)) },
            onSignedOut = onSignedOut,
            contentPadding = padding,
        )
    }
}
