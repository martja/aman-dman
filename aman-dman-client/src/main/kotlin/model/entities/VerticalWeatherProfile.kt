package org.example.model.entities

import kotlinx.datetime.Instant
import org.example.LatLng

data class WindData(
    val directionDeg: Int,
    val speedKts: Int
)

data class WeatherData(
    val flightLevelFt: Int,
    val temperatureC: Int,
    val wind: WindData
)

data class VerticalWeatherProfile(
    val time: Instant,
    val position: LatLng,
    val weatherData: MutableList<WeatherData>
)