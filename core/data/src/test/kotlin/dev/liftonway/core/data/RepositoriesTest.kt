package dev.liftonway.core.data

import dev.liftonway.core.model.CardBrand
import dev.liftonway.core.model.NotificationKind
import dev.liftonway.core.model.Person
import dev.liftonway.core.model.RideQuery
import dev.liftonway.core.model.TransactionType
import dev.liftonway.core.model.TripStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A fixed clock (10 October 2026, 09:00) and virtual time, so delays cost nothing and results never drift. */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-10T09:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 10)

    private fun TestScope.rides() = FakeRideRepository(clock, backgroundScope, FakeVehicleRepository(), CurrentPerson { Person("me", "Test", null) })
    private fun query(rides: FakeRideRepository, date: LocalDate = today.plusDays(1), from: String = "f7", to: String = "saddar") =
        RideQuery(rides.place(from), rides.place(to), date, seats = 1)

    @Test
    fun `search finds rides inside the week and none beyond it`() = runTest {
        val rides = rides()
        assertTrue(rides.search(query(rides)).isNotEmpty())
        assertTrue(rides.search(query(rides, date = today.plusDays(FakeRideRepository.SEARCH_WINDOW_DAYS + 1))).isEmpty())
    }

    @Test
    fun `search never offers a ride that has already left`() = runTest {
        val rides = rides()
        val found = rides.search(query(rides, date = today))
        assertTrue(found.all { it.departure.isAfter(rides.now().plusMinutes(29)) })
    }

    @Test
    fun `stops stay in the cities the ride runs between`() = runTest {
        val rides = rides()
        (0L..FakeRideRepository.SEARCH_WINDOW_DAYS).forEach { day ->
            rides.search(query(rides, date = today.plusDays(day), from = "blue", to = "bahria")).forEach { ride ->
                assertTrue(ride.stops.all { it.area == "Islamabad" || it.area == "Rawalpindi" })
            }
        }
    }

    @Test
    fun `recommendations skip the searched date and repeat no driver`() = runTest {
        val rides = rides()
        val q = query(rides, date = today.plusDays(20))
        val recommended = rides.recommended(q)
        assertTrue(recommended.isNotEmpty())
        assertTrue(recommended.none { it.departure.toLocalDate() == q.date })
        assertEquals(recommended.size, recommended.map { it.driver.id }.distinct().size)
    }

    @Test
    fun `a request starts pending and the driver confirms it a few seconds later`() = runTest {
        val rides = rides()
        val ride = rides.search(query(rides)).first()
        val booking = rides.request(ride, seats = 1)
        assertFalse(rides.bookings.value.first { it.id == booking.id }.confirmed)
        advanceTimeBy(6_001)
        runCurrent()
        assertTrue(rides.bookings.value.first { it.id == booking.id }.confirmed)
    }

    @Test
    fun `cancelling is refused within four hours and allowed before that`() = runTest {
        val rides = rides()
        val soon = rides.bookings.value.first { it.id == "b2" } // Departs in about two hours.
        val tomorrow = rides.bookings.value.first { it.id == "b1" }
        assertEquals(CancelResult.TooLate, rides.cancel(soon.id))
        assertEquals(CancelResult.Cancelled, rides.cancel(tomorrow.id))
        assertEquals(TripStatus.Cancelled, rides.bookings.value.first { it.id == tomorrow.id }.status)
        assertEquals(TripStatus.Upcoming, rides.bookings.value.first { it.id == soon.id }.status)
    }

    @Test
    fun `top up adds credit and a history entry`() = runTest {
        val wallet = WalletRepository(clock)
        val card = wallet.wallet.value.cards.first()
        wallet.topUp(2_000, card.id)
        assertEquals(2_000L, wallet.wallet.value.balance)
        val latest = wallet.wallet.value.transactions.first()
        assertEquals(TransactionType.TopUp, latest.type)
        assertEquals(today, latest.date)
    }

    @Test
    fun `paying more than the balance fails and changes nothing`() = runTest {
        val wallet = WalletRepository(clock)
        val before = wallet.wallet.value
        assertFalse(wallet.pay("Hamza Tariq", 450))
        assertEquals(before, wallet.wallet.value)
    }

    @Test
    fun `a saved card keeps only its brand and last four digits`() = runTest {
        val wallet = WalletRepository(clock)
        val card = wallet.addCard("4242 4242 4242 4242")
        assertEquals(CardBrand.Visa, card.brand)
        assertEquals("4242", card.last4)
        assertFalse(wallet.wallet.value.toString().contains("424242424242"))
    }

    @Test
    fun `confirming a payment request needs enough credit`() = runTest {
        val wallet = WalletRepository(clock)
        val notifications = NotificationRepository(clock, wallet)
        val request = notifications.items.value.first { it.kind == NotificationKind.PaymentRequest }

        assertEquals(AnswerResult.NotEnoughCredit, notifications.answer(request.id, accept = true))
        assertFalse(notifications.items.value.first { it.id == request.id }.answered)

        wallet.topUp(1_000, wallet.wallet.value.cards.first().id)
        assertEquals(AnswerResult.Done, notifications.answer(request.id, accept = true))
        assertTrue(notifications.items.value.first { it.id == request.id }.answered)
        assertEquals(1_000L - (request.amount ?: 0), wallet.wallet.value.balance)
    }
}
