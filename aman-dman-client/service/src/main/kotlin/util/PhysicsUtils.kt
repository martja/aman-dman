package org.example.util

import org.example.Wind
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PhysicsUtils {
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
        val pressure = P0 * (1 - (L * altitudeMeters) / T0).pow(g / (R * L))

        // Air density at altitude
        val rho = pressure / (R * tempKelvin)
        val rho0 = P0 / (R * T0)

        // TAS estimation
        return (ias * sqrt(rho0 / rho)).roundToInt()
    }

    fun tasToIAS(tas: Int, altitudeFt: Int, tempCelsius: Int): Int {
        val tempKelvin = tempCelsius + 273.15
        val altitudeMeters = altitudeFt * 0.3048

        // Constants
        val P0 = 101325.0
        val T0 = 288.15
        val L = 0.0065
        val R = 287.05
        val g = 9.80665

        // ISA pressure at altitude
        val pressure = P0 * (1 - (L * altitudeMeters) / T0).pow(g / (R * L))

        // Air density
        val rho = pressure / (R * tempKelvin)
        val rho0 = P0 / (R * T0)

        // IAS = TAS * sqrt(ρ / ρ0)
        return (tas * sqrt(rho / rho0)).roundToInt()
    }

    fun gsToTas(gs: Int, wind: Wind, track: Int): Int {
        val windAngleRad = Math.toRadians((wind.directionDeg - track).toDouble())
        val windSpeed = wind.speedKts.toDouble()
        val gsDouble = gs.toDouble()

        // Quadratic equation: TAS² - 2*TAS*wind*cos(angle) + wind² - GS² = 0
        val a = 1.0
        val b = -2 * windSpeed * cos(windAngleRad)
        val c = windSpeed.pow(2) - gsDouble.pow(2)

        val discriminant = b * b - 4 * a * c

        if (discriminant < 0) {
            // No real solution, return GS as fallback
            return gs
        }

        val tas = (-b + sqrt(discriminant)) / (2 * a)
        return tas.roundToInt()
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

    fun machToTAS(mach: Float, altitudeFt: Int): Int {
        return (mach * speedOfSoundAtAltitude(altitudeFt)).roundToInt()
    }

    fun machToIAS(mach: Float, altitudeFt: Int, temperatureC: Int): Int {
        val tas = machToTAS(mach, altitudeFt)
        return tasToIAS(tas, altitudeFt, temperatureC)
    }

    private fun speedOfSoundAtAltitude(altitudeFt: Int): Float {
        val altitudeM = altitudeFt * 0.3048f
        return if (altitudeM < 11000) {
            val temperature = 288.15f - 0.0065f * altitudeM  // ISA lapse rate
            20.0457f * sqrt(temperature)  // a = sqrt(γ * R * T), simplified
        } else {
            // Isothermal layer above 11km (standard temp 216.65 K)
            295.07f  // approx 295.07 m/s
        }
    }
}