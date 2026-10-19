package dev.liftonway.core.data.remote

import dev.liftonway.core.model.Booking
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
data class BookingDto(val id: String, val ride: RideDto, val seats: Int, val status: String, val tripStatus: String, val canCancel: Boolean)

@Serializable
data class PassengerDto(val bookingId: String, val name: String, val seats: Int, val status: String)

@Serializable
data class PublishedRideDto(val ride: RideDto, val tripStatus: String, val passengers: List<PassengerDto>)

@Serializable
data class MeDto(val id: String, val name: String, val phone: String, val photoUrl: String? = null, val rating: Double? = null, val hasVehicle: Boolean)

@Serializable
data class SessionDto(val token: String, val expiresAt: String, val created: Boolean, val user: MeDto)

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

internal fun BookingDto.toModel() = Booking(id, ride.toModel(), seats, tripStatus(tripStatus), confirmed = status == "CONFIRMED")

internal fun PublishedRideDto.toModel() =
    PublishedRide(ride.toModel(), tripStatus(tripStatus), passengers.map { Person(it.bookingId, it.name, rating = null) })

internal fun VehicleDto.toModel() = Vehicle(name, model, plate, colour, airConditioning, heater)

internal fun Vehicle.toDto() = VehicleDto(name, model, plate, color, airConditioning, heater)
