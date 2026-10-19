package dev.liftonway.feature.trips

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.liftonway.core.data.CurrentPerson
import dev.liftonway.core.data.FakeRideRepository
import dev.liftonway.core.data.FakeVehicleRepository
import dev.liftonway.core.designsystem.theme.LiftTheme
import dev.liftonway.core.model.Person
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock

/** The 4-hour rule as the user meets it: the cancel sheet greys out Cancel for a ride that leaves soon. */
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

    private fun show() = compose.setContent {
        LiftTheme { RequestedRidesRoute(onBack = {}, onBookRide = {}, viewModel = TripsViewModel(rides)) }
    }

    @Test
    fun ride_leaving_soon_cannot_be_cancelled() {
        show()
        compose.onNodeWithText("AYESHA MALIK").performClick() // Seeded to leave in about two hours.
        compose.onNodeWithText("Ride can only be canceled before 4 hours.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun ride_tomorrow_can_be_cancelled() {
        show()
        compose.onNodeWithText("HAMZA TARIQ").performClick() // Seeded for tomorrow morning.
        compose.onNodeWithText("You can cancel until 4 hours before departure.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsEnabled()
    }
}
