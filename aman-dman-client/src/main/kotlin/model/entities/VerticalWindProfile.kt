package org.example.model.entities

import kotlinx.datetime.Instant

data class WindInformation(
    val flightLevel: Int,
    val windDirection: Int,
    val windSpeed: Int,
    val temperature: Int
)

data class VerticalWindProfile(
    val time: Instant,
    val latitude: Double,
    val longitude: Double,
    val windInformation: MutableList<WindInformation>
)