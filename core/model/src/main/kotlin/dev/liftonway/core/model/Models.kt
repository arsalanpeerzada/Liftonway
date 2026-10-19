package dev.liftonway.core.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime

/** Amounts are whole Pakistani rupees; the app shows no paisa. */
typealias Rupees = Long

@Immutable
data class Place(val id: String, val title: String, val area: String) {
    val label: String get() = "$title, $area"
}

/** [rating] is null until someone has been rated. */
@Immutable
data class Person(val id: String, val name: String, val rating: Float?, val photoUri: String? = null) {
    val initials: String get() = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

@Immutable
data class Vehicle(
    val name: String,
    val model: String,
    val plate: String,
    val color: String,
    val airConditioning: Boolean,
    val heater: Boolean,
    val photoUri: String? = null,
) {
    /** "Green Toyota Corolla - ABC 123", as on the Publish a ride screen. */
    val summary: String get() = listOf(color, name).filter { it.isNotBlank() }.joinToString(" ") + " - $plate"
}

@Immutable
data class Ride(
    val id: String,
    val driver: Person,
    val departure: LocalDateTime,
    val pickup: Place,
    val destination: Place,
    val stops: List<Place>,
    val vehicleName: String,
    val totalSeats: Int,
    val pricePerSeat: Rupees,
    val route: String = "",
) {
    /** Pickup, any stops, then the destination. */
    val allStops: List<Place> get() = listOf(pickup) + stops + destination
}

enum class TripStatus { Upcoming, Completed, Cancelled }

/** A seat request the user made on someone else's ride. */
@Immutable
data class Booking(val id: String, val ride: Ride, val seats: Int, val status: TripStatus, val confirmed: Boolean)

/** A ride the user published as the driver. */
@Immutable
data class PublishedRide(val ride: Ride, val status: TripStatus, val passengers: List<Person>)

@Immutable
data class RideQuery(val pickup: Place, val destination: Place, val date: LocalDate, val seats: Int)

@Immutable
data class RideDraft(
    val pickup: Place?,
    val destination: Place?,
    val stops: List<Place>,
    val date: LocalDate,
    val time: java.time.LocalTime,
    val seats: Int,
    val route: String,
)

@Immutable
data class UserProfile(val name: String, val countryCode: String, val phone: String, val photoUri: String?) {
    val displayPhone: String get() = "$countryCode $phone"
}

enum class NotificationKind { RideRequest, PaymentRequest, PaymentSent, PaymentReceived }

@Immutable
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val personName: String,
    val message: String,
    val at: LocalDateTime,
    val amount: Rupees? = null,
    val answered: Boolean = false,
)

enum class CardBrand { Mastercard, Visa }

/** Only the brand and the last four digits ever reach the app's state; a real card goes to the payment provider. */
@Immutable
data class PaymentCard(val id: String, val brand: CardBrand, val last4: String)

enum class TransactionType { Received, Sent, TopUp }

@Immutable
data class Transaction(val id: String, val counterparty: String, val amount: Rupees, val type: TransactionType, val date: LocalDate)

@Immutable
data class Wallet(val balance: Rupees, val cards: List<PaymentCard>, val transactions: List<Transaction>)
