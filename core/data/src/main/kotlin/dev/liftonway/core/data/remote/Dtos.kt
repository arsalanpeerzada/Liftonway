package dev.liftonway.core.data.remote

import dev.liftonway.core.model.AppNotification
import dev.liftonway.core.model.Booking
import dev.liftonway.core.model.NotificationKind
import dev.liftonway.core.model.Passenger
import dev.liftonway.core.model.Person
import dev.liftonway.core.model.Place
import dev.liftonway.core.model.PublishedRide
import dev.liftonway.core.model.Ride
import dev.liftonway.core.model.TripStatus
import dev.liftonway.core.model.Vehicle
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** The zone the backend and the app agree ride times are shown in. */
internal val PakistanTime: ZoneId = ZoneId.of("Asia/Karachi")

@Serializable
data class Problem(val code: String? = null, val detail: String? = null)

@Serializable
data class PlaceDto(val id: String, val title: String, val area: String)

@Serializable
data class DriverDto(val id: String, val name: String, val rating: Double? = null, val photoUrl: String? = null)

@Serializable
data class RideDto(
    val id: String,
    val driver: DriverDto,
    val departure: String,
    val points: List<PlaceDto>,
    val vehicleName: String,
    val seatsTotal: Int,
    val seatsLeft: Int,
    val pricePerSeat: Int,
    val route: String = "",
)

@Serializable
data class VehicleBriefDto(
    val name: String,
    val model: String,
    val colour: String,
    val airConditioning: Boolean,
    val heater: Boolean,
    val plate: String? = null,
)

@Serializable
data class BookingDto(
    val id: String,
    val ride: RideDto,
    val seats: Int,
    val status: String,
    val tripStatus: String,
    val canCancel: Boolean,
    val vehicle: VehicleBriefDto? = null,
    val myRating: Int? = null,
    val canRate: Boolean = false,
)

@Serializable
data class RateBody(val stars: Int)

@Serializable
data class PassengerDto(
    val bookingId: String,
    val passengerId: String,
    val name: String,
    val photoUrl: String? = null,
    val seats: Int,
    val status: String,
    val myRating: Int? = null,
    val canRate: Boolean = false,
)

@Serializable
data class PublishedRideDto(
    val ride: RideDto,
    val tripStatus: String,
    val passengers: List<PassengerDto>,
    val canCancel: Boolean = false,
    val canComplete: Boolean = false,
)

@Serializable
data class MeDto(val id: String, val name: String, val phone: String, val photoUrl: String? = null, val rating: Double? = null, val hasVehicle: Boolean)

@Serializable
data class SessionDto(
    val token: String,
    val expiresAt: String,
    val refreshToken: String,
    val refreshExpiresAt: String,
    val created: Boolean,
    val user: MeDto,
)

@Serializable
data class RefreshBody(val refreshToken: String)

@Serializable
data class LogoutBody(val refreshToken: String, val deviceToken: String? = null)

@Serializable
data class DeviceBody(val token: String)

@Serializable
data class RenewedDto(val token: String, val expiresAt: String, val refreshToken: String, val refreshExpiresAt: String)

@Serializable
data class VehicleDto(val name: String, val model: String, val plate: String, val colour: String, val airConditioning: Boolean, val heater: Boolean)

@Serializable
data class CodeRequest(val countryCode: String, val number: String)

@Serializable
data class CodeVerify(val countryCode: String, val number: String, val code: String, val name: String? = null)

@Serializable
data class NameBody(val name: String)

@Serializable
data class SeatsBody(val seats: Int)

@Serializable
data class PublishBody(
    val from: String,
    val to: String,
    val stops: List<String>,
    val departure: String,
    val seats: Int,
    val pricePerSeat: Int,
    val route: String,
)

internal fun PlaceDto.toModel() = Place(id, title, area)

internal fun String.toPakistanTime(): LocalDateTime = OffsetDateTime.parse(this).atZoneSameInstant(PakistanTime).toLocalDateTime()

internal fun LocalDateTime.toApiTime(): String = atZone(PakistanTime).toOffsetDateTime().toString()

internal fun RideDto.toModel() = Ride(
    id = id,
    driver = Person(driver.id, driver.name, driver.rating?.toFloat(), driver.photoUrl),
    departure = departure.toPakistanTime(),
    pickup = points.first().toModel(),
    destination = points.last().toModel(),
    stops = points.drop(1).dropLast(1).map { it.toModel() },
    vehicleName = vehicleName,
    totalSeats = seatsTotal,
    pricePerSeat = pricePerSeat.toLong(),
    route = route,
)

internal fun tripStatus(value: String) = when (value) {
    "COMPLETED" -> TripStatus.Completed
    "CANCELLED" -> TripStatus.Cancelled
    else -> TripStatus.Upcoming
}

internal fun BookingDto.toModel() = Booking(
    id,
    ride.toModel(),
    seats,
    tripStatus(tripStatus),
    confirmed = status == "CONFIRMED",
    vehicle = vehicle?.let { Vehicle(it.name, it.model, it.plate.orEmpty(), it.colour, it.airConditioning, it.heater) },
    myRating = myRating,
    canRate = canRate,
)

internal fun PublishedRideDto.toModel() = PublishedRide(
    ride.toModel(),
    tripStatus(tripStatus),
    passengers.map {
        Passenger(it.bookingId, Person(it.passengerId, it.name, rating = null, photoUri = it.photoUrl), it.seats, it.status == "CONFIRMED", it.myRating, it.canRate)
    },
    canCancel = canCancel,
    canComplete = canComplete,
)

internal fun VehicleDto.toModel() = Vehicle(name, model, plate, colour, airConditioning, heater)

internal fun Vehicle.toDto() = VehicleDto(name, model, plate, color, airConditioning, heater)

@Serializable
data class ActorDto(val id: String, val name: String, val photoUrl: String? = null)

@Serializable
data class RideBriefDto(val id: String, val from: String, val to: String, val departure: String)

@Serializable
data class NotificationDto(
    val id: String,
    val kind: String,
    val actor: ActorDto? = null,
    val ride: RideBriefDto? = null,
    val bookingId: String? = null,
    val seats: Int? = null,
    val bookingStatus: String? = null,
    val actionable: Boolean = false,
    val read: Boolean = false,
    val createdAt: String,
)

/** Place titles run long ("Blue Area, Jinnah Avenue"); messages use the part before the comma. */
private fun String.short() = substringBefore(',').trim()

/** Null for kinds this version of the app doesn't know yet. */
internal fun NotificationDto.toModel(): AppNotification? {
    val kind = when (kind) {
        "RIDE_REQUEST" -> NotificationKind.RideRequest
        "REQUEST_ACCEPTED" -> NotificationKind.RequestAccepted
        "REQUEST_DECLINED" -> NotificationKind.RequestDeclined
        "REQUEST_WITHDRAWN" -> NotificationKind.RequestWithdrawn
        "RIDE_CANCELLED" -> NotificationKind.RideCancelled
        "RIDE_COMPLETED" -> NotificationKind.RideCompleted
        else -> return null
    }
    val trip = ride?.let { " from ${it.from.short()} to ${it.to.short()}" }.orEmpty()
    val message = when (kind) {
        NotificationKind.RideRequest -> if ((seats ?: 1) > 1) "wants $seats seats on your ride$trip" else "wants to have a ride with you$trip"
        NotificationKind.RequestAccepted -> "accepted your request for the ride$trip"
        NotificationKind.RequestDeclined -> "declined your request for the ride$trip"
        NotificationKind.RequestWithdrawn -> "canceled their request for your ride$trip"
        NotificationKind.RideCancelled -> "canceled the ride$trip"
        else -> "completed the ride$trip"
    }
    // How an answered seat request ended, from the driver's side.
    val outcome = if (kind == NotificationKind.RideRequest && !actionable) {
        when (bookingStatus) {
            "CONFIRMED" -> "Accepted"
            "DECLINED" -> "Declined"
            "CANCELLED" -> "Withdrawn"
            else -> "Closed"
        }
    } else {
        null
    }
    return AppNotification(
        id = id,
        kind = kind,
        personName = actor?.name ?: "Someone",
        message = message,
        at = createdAt.toPakistanTime(),
        actionable = actionable,
        outcome = outcome,
        bookingId = bookingId,
        rideId = ride?.id,
        read = read,
    )
}
