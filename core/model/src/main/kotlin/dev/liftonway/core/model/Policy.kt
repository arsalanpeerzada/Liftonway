package dev.liftonway.core.model

import java.time.Duration
import java.time.LocalDateTime

/** Business rules shared by the screens and the data layer. */
object Policy {
    /** A booked ride can't be cancelled once departure is closer than this. */
    val CancelCutoff: Duration = Duration.ofHours(4)

    const val MAX_SEATS = 6

    /** Fares per seat the server accepts, in whole rupees, and the step the picker moves in. */
    const val MIN_FARE: Rupees = 50
    const val MAX_FARE: Rupees = 50_000
    const val FARE_STEP: Rupees = 50
    const val DEFAULT_FARE: Rupees = 500

    fun canCancel(departure: LocalDateTime, now: LocalDateTime): Boolean = Duration.between(now, departure) >= CancelCutoff
}
