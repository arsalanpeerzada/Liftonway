package dev.liftonway.core.model

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rupeeDigits: NumberFormat = NumberFormat.getIntegerInstance(Locale.UK)

/** "PKR 1,200" */
fun Rupees.asPkr(): String = "PKR ${rupeeDigits.format(this)}"

private val longDate = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.UK)
private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.UK)
private val chipDate = DateTimeFormatter.ofPattern("EEE. dd MMM", Locale.UK)
private val upperDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
private val dayMonthTime = DateTimeFormatter.ofPattern("d MMMM h:mm a", Locale.UK)

/** "April 3, 2026 | 8:00 AM", the ride card header. */
fun LocalDateTime.rideHeader(): String = "${format(longDate)} | ${format(time).uppercase()}"

/** "Thu. 05 Nov", the date chip in the forms. */
fun LocalDate.chip(): String = format(chipDate)

/** "22 SEPTEMBER 2026", the transaction history label. */
fun LocalDate.upperLabel(): String = format(upperDate).uppercase(Locale.UK)

/** "14 August 2:30 pm", in notifications. */
fun LocalDateTime.notificationStamp(): String = format(dayMonthTime).replace("AM", "am").replace("PM", "pm")

fun java.time.LocalTime.clock(): String = format(time).uppercase()
