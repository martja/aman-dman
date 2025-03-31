package org.example.model.entities.weather

import kotlinx.datetime.Instant
import org.example.model.entities.navdata.LatLng
import kotlin.math.roundToInt

data class VerticalWeatherProfile(
    val time: Instant,
    val position: LatLng,
    val weatherLayers: MutableList<WeatherLayer>
) {
    fun interpolateWeatherAtAltitude(altitudeFt: Int): WeatherLayer {
        // Interpolate wind data based on the two closest altitudes
        val sorted = weatherLayers.sortedBy { it.flightLevelFt }
        val lower = sorted.last { it.flightLevelFt <= altitudeFt }
        val upper = sorted.first { it.flightLevelFt > altitudeFt }

        val ratio = (altitudeFt - lower.flightLevelFt).toFloat() / (upper.flightLevelFt - lower.flightLevelFt).toFloat()

        val direction = (1 - ratio) * lower.wind.directionDeg + ratio * upper.wind.directionDeg
        val speed = (1 - ratio) * lower.wind.speedKts + ratio * upper.wind.speedKts
        val temperature = (1 - ratio) * lower.temperatureC + ratio * upper.temperatureC

        return WeatherLayer(
            flightLevelFt = altitudeFt,
            wind = Wind(direction.roundToInt(), speed.roundToInt()),
            temperatureC = temperature.roundToInt()
        )
    }
}