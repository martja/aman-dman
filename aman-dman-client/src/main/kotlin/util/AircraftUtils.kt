package org.example.util

import org.example.model.entities.weather.Wind
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AircraftUtils {
    fun iasToTas(ias: Int, altitudeFt: Int, tempCelsius: Int): Int {
        val tempKelvin = tempCelsius + 273.15
        val altitudeMeters = altitudeFt * 0.3048

        // Constants
        val P0 = 101325.0 // Sea level pressure in Pascals
        val T0 = 288.15   // Sea level temperature in Kelvin
        val L = 0.0065    // Temperature lapse rate (K/m)
        val R = 287.05    // Specific gas constant for dry air (J/kg·K)
        val g = 9.80665   // Gravity (m/s²)

        // ISA Pressure at altitude
        val pressure = P0 * Math.pow(1 - (L * altitudeMeters) / T0, g / (R * L))

        // Air density at altitude
        val rho = pressure / (R * tempKelvin)
        val rho0 = P0 / (R * T0)

        // TAS estimation
        return (ias * Math.sqrt(rho0 / rho)).roundToInt()
    }

    fun tasToGs(tas: Int, wind: Wind, track: Int): Int {
        val windAngleRad = Math.toRadians((wind.directionDeg - track).toDouble())
        val windSpeed = wind.speedKts.toDouble()

        val gs = sqrt(
            tas.toDouble().pow(2) + windSpeed.pow(2) -
                    2 * tas * windSpeed * cos(windAngleRad)
        )

        return gs.roundToInt()
    }
}