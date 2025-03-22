package org.example.model.entities

import kotlinx.datetime.Instant
import org.example.LatLng

data class WeatherData(
    val flightLevelFt: Int,
    val windDirectionKts: Int,
    val windSpeedKts: Int,
    val temperatureC: Int
)

data class VerticalWeatherProfile(
    val time: Instant,
    val position: LatLng,
    val weatherData: MutableList<WeatherData>
)