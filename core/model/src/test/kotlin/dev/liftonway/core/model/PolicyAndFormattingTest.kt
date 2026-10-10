package dev.liftonway.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PolicyAndFormattingTest {
    private val departure = LocalDateTime.of(2026, 10, 11, 8, 0)

    @Test
    fun `can cancel up to exactly four hours before departure`() {
        assertTrue(Policy.canCancel(departure, departure.minusHours(5)))
        assertTrue(Policy.canCancel(departure, departure.minusHours(4)))
    }

    @Test
    fun `cannot cancel inside four hours or after departure`() {
        assertFalse(Policy.canCancel(departure, departure.minusHours(4).plusMinutes(1)))
        assertFalse(Policy.canCancel(departure, departure.minusMinutes(10)))
        assertFalse(Policy.canCancel(departure, departure.plusHours(1)))
    }

    @Test
    fun `rupees use thousands separators`() {
        assertEquals("PKR 0", 0L.asPkr())
        assertEquals("PKR 1,200", 1_200L.asPkr())
        assertEquals("PKR 25,000", 25_000L.asPkr())
    }

    @Test
    fun `dates read as in the designs`() {
        assertEquals("October 11, 2026 | 8:00 AM", departure.rideHeader())
        assertEquals("Sun. 11 Oct", LocalDate.of(2026, 10, 11).chip())
        assertEquals("22 SEPTEMBER 2026", LocalDate.of(2026, 9, 22).upperLabel())
        assertEquals("14 August 2:30 pm", LocalDateTime.of(2026, 8, 14, 14, 30).notificationStamp())
        assertEquals("12:00 AM", LocalTime.MIDNIGHT.clock())
    }

    @Test
    fun `initials come from the first two names`() {
        assertEquals("HT", Person("1", "Hamza Tariq", 4.8f).initials)
        assertEquals("A", Person("2", "Ayesha", 4.8f).initials)
    }

    @Test
    fun `vehicle summary matches the publish screen`() {
        val car = Vehicle("Honda Civic", "2021", "LEA 4521", "White", airConditioning = true, heater = false)
        assertEquals("White Honda Civic - LEA 4521", car.summary)
    }
}
