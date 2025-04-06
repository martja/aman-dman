package org.example.entities.weather

import org.example.VerticalWeatherProfile
import org.example.WeatherLayer
import org.example.Wind
import kotlin.math.roundToInt

fun VerticalWeatherProfile.interpolateWeatherAtAltitude(altitudeFt: Int): WeatherLayer {
    // Interpolate wind data based on the two closest altitudes
    val sorted = weatherLayers.sortedBy { it.flightLevelFt }
    val lower = sorted.lastOrNull { it.flightLevelFt <= altitudeFt } ?: sorted.minBy { it.flightLevelFt }
    val upper = sorted.firstOrNull { it.flightLevelFt > altitudeFt } ?: sorted.maxBy { it.flightLevelFt }

    val ratio =
        if (altitudeFt <= lower.flightLevelFt) 0f
        else if (altitudeFt >= upper.flightLevelFt) 1f
        else (altitudeFt - lower.flightLevelFt).toFloat() / (upper.flightLevelFt - lower.flightLevelFt).toFloat()

    val direction = (1 - ratio) * lower.wind.directionDeg + ratio * upper.wind.directionDeg
    val speed = (1 - ratio) * lower.wind.speedKts + ratio * upper.wind.speedKts
    val temperature = (1 - ratio) * lower.temperatureC + ratio * upper.temperatureC

    return WeatherLayer(
        flightLevelFt = altitudeFt,
        wind = Wind(direction.roundToInt(), speed.roundToInt()),
        temperatureC = temperature.roundToInt()
    )
}
