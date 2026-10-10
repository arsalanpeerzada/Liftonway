package dev.liftonway.core.data

import dev.liftonway.core.model.Person
import dev.liftonway.core.model.Place

/**
 * Made-up people and public landmarks for the prototype. No real users' names, numbers or photos.
 */
internal object SampleData {
    val places = listOf(
        Place("f7", "F-7 Markaz", "Islamabad"),
        Place("blue", "Blue Area, Jinnah Avenue", "Islamabad"),
        Place("g11", "G-11 Markaz", "Islamabad"),
        Place("dha2", "DHA Phase 2", "Islamabad"),
        Place("airport", "Islamabad International Airport", "Islamabad"),
        Place("faizabad", "Faizabad Interchange", "Islamabad"),
        Place("saddar", "Saddar", "Rawalpindi"),
        Place("bahria", "Bahria Town Phase 7", "Rawalpindi"),
        Place("daewoo", "Daewoo Terminal, Peshawar Road", "Rawalpindi"),
        Place("liberty", "Liberty Market, Gulberg", "Lahore"),
        Place("mall", "Mall Road", "Lahore"),
        Place("dhaLhr", "DHA Phase 5", "Lahore"),
        Place("university", "University Town", "Peshawar"),
        Place("clifton", "Clifton Block 5", "Karachi"),
    )

    val drivers = listOf(
        Person("d1", "Hamza Tariq", 4.8f),
        Person("d2", "Ayesha Malik", 4.9f),
        Person("d3", "Bilal Ahmed", 4.6f),
        Person("d4", "Sana Iqbal", 4.7f),
        Person("d5", "Usman Raza", 4.5f),
        Person("d6", "Mahnoor Khan", 4.8f),
    )

    val vehicles = listOf("Toyota Corolla", "Honda Civic", "Suzuki Cultus", "Toyota Yaris", "Honda City", "Kia Sportage")

    fun place(id: String) = places.first { it.id == id }
}
