package dev.liftonway.core.model

import java.time.Duration
import java.time.LocalDateTime

/** Business rules shared by the screens and the data layer. */
object Policy {
    /** A booked ride can't be cancelled once departure is closer than this. */
    val CancelCutoff: Duration = Duration.ofHours(4)

    const val MAX_SEATS = 6

    fun canCancel(departure: LocalDateTime, now: LocalDateTime): Boolean = Duration.between(now, departure) >= CancelCutoff
}
