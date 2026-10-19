package dev.liftonway.feature.trips

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.liftonway.core.data.CurrentPerson
import dev.liftonway.core.data.FakeRideRepository
import dev.liftonway.core.data.FakeVehicleRepository
import dev.liftonway.core.designsystem.theme.LiftTheme
import dev.liftonway.core.model.Person
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.time.Clock
import java.util.concurrent.TimeUnit

/** A requested ride's details as the user meets them: the 4-hour rule, and rating after the trip. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RequestedRidesTest {
    @get:Rule val compose = createComposeRule()

    private val rides = FakeRideRepository(
        Clock.systemDefaultZone(),
        CoroutineScope(Dispatchers.Unconfined),
        FakeVehicleRepository(),
        CurrentPerson { Person("me", "Test", null) },
    )

    private fun show(bookingId: String) = compose.setContent {
        LiftTheme { BookingDetailsRoute(bookingId, onBack = {}, viewModel = BookingDetailsViewModel(bookingId, rides)) }
    }

    @Test
    fun ride_leaving_soon_cannot_be_cancelled() {
        show("b2") // Seeded to leave in about two hours, seat confirmed.
        compose.onNodeWithText("Ride can only be canceled before 4 hours.").assertIsDisplayed()
        compose.onNodeWithText("Cancel seat").assertIsNotEnabled()
        compose.onNodeWithText("ICT 7781").performScrollTo().assertIsDisplayed() // Confirmed, so the plate is shown.
    }

    @Test
    fun ride_tomorrow_can_be_cancelled() {
        show("b1") // Seeded for tomorrow morning, still pending.
        compose.onNodeWithText("Number plate shown once the driver confirms").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancel request").performClick()
        compose.onNodeWithText("You can cancel until 4 hours before departure.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsEnabled()
    }

    @Test
    fun completed_ride_can_be_rated_once() {
        show("b3")
        compose.onNodeWithText("Rate Bilal").performClick()
        compose.onNodeWithText("Submit").assertIsNotEnabled()
        compose.onNodeWithContentDescription("4 stars").performClick()
        compose.onNodeWithText("Great").assertIsDisplayed()
        compose.onNodeWithText("Submit").performClick()
        // The demo repository pauses like a network call; let the main looper's clock run past it.
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        compose.waitForIdle()
        assertEquals(4, rides.bookings.value.first { it.id == "b3" }.myRating)
        compose.onNodeWithText("Thanks for rating!").assertIsDisplayed()
    }
}
