package org.example.util

import org.example.Wind
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PhysicsUtils {
    /**
     * Convert Indicated Airspeed (IAS) to True Airspeed (TAS)
     */
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

    /**
     * Convert Mach number to Indicated Airspeed (IAS)
     */
    fun machToIAS(mach: Float, altitudeFt: Int, satCelsius: Int): Int {
        val satKelvin = satCelsius + 273.15
        val tatKelvin = satKelvin * (1.0 + 0.2 * mach * mach)
        val speedOfSound = sqrt(1.4 * 287.05 * tatKelvin)  // m/s
        val tas = mach * speedOfSound
        val tasKts = tas * 1.94384
        val cas = tasToCAS(tasKts, altitudeFt)
        return cas.roundToInt()
    }

    /**
     * Convert True Airspeed (TAS) to Calibrated Airspeed (CAS)
     */
    private fun tasToCAS(tasKts: Double, altitudeFt: Int): Double {
        val P0 = 101325.0     // Sea-level pressure (Pa)
        val T0 = 288.15       // Sea-level temp (K)
        val gamma = 1.4
        val R = 287.05

        val altitudeM = altitudeFt * 0.3048
        val L = 0.0065
        val T = T0 - L * altitudeM
        val P = P0 * (1 - (L * altitudeM) / T0).pow(5.2561)

        val a = sqrt(gamma * R * T)
        val tas = tasKts / 1.94384  // knots → m/s
        val M = tas / a

        // Calculate impact pressure (q_c) for TAS (compressible flow)
        val q_c = P * ((1 + ((gamma - 1) / 2) * M * M).pow(gamma / (gamma - 1)) - 1)

        // Now invert q_c to get CAS (at sea level)
        val cas = sqrt((2 * P0 / (gamma - 1)) *
                ((q_c / P0 + 1).pow((gamma - 1) / gamma) - 1))

        return cas * 1.94384  // m/s → knots
    }

    /**
     * Convert True Airspeed (TAS) to Indicated Airspeed (IAS)
     */
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

    /**
     * Convert Ground Speed (GS) to True Airspeed (TAS) based on wind direction and flown track
     */
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

    /**
     * Convert True Airspeed (TAS) to Ground Speed (GS) based on wind direction and flown track
     */
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