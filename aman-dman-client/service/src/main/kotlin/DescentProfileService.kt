package org.example

import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.Star
import org.example.entities.navigation.star.StarFix
import org.example.util.PhysicsUtils.iasToTas
import org.example.util.PhysicsUtils.tasToGs
import org.example.util.NavigationUtils.interpolatePositionAlongPath
import org.example.util.PhysicsUtils.machToIAS
import org.example.util.WeatherUtils.getStandardTemperaturAt
import org.example.util.WeatherUtils.interpolateWeatherAtAltitude
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

object DescentProfileService {

    val defaultWind = Wind(0, 0)

    fun List<RoutePoint>.generateDescentSegments(
        aircraftPosition: AircraftPosition,
        verticalWeatherProfile: VerticalWeatherProfile?,
        star: Star?,
        aircraftPerformance: AircraftPerformance,
        landingAirportIcao: String
    ): List<EstimatedProfilePoint> {
        val starMap = star?.fixes?.associateBy { it.id }
        val estimatedProfilePoints = mutableListOf<EstimatedProfilePoint>()
        val lastAltitudeConstraint = star?.findFinalAltitude()

        // Starts at the airports and works backwards
        var probePosition = this.last().position
        var probeAltitude = lastAltitudeConstraint ?: 0
        var probingDistance = 0f
        var accumulatedTimeFromDestination = 0.seconds

        estimatedProfilePoints.add(
            EstimatedProfilePoint(
                inbound = this.last().id,
                position = probePosition,
                altitude = probeAltitude,
                remainingDistance = probingDistance,
                remainingTime = accumulatedTimeFromDestination,
                groundSpeed = aircraftPerformance.landingVat, // TODO: convert to ground speed
                tas = aircraftPerformance.landingVat,
                wind = defaultWind, // TODO: use wind from METAR
                ias = aircraftPerformance.landingVat,
                heading =
                    if (this.size >= 2)
                        this[lastIndex - 1].position.bearingTo(this[lastIndex].position)
                    else 0
            )
        )

        // Start from the last waypoint (the airport) and work backward
        for (i in lastIndex downTo 1) {
            val laterPoint = this[i]
            val earlierPoint = this[i-1]
            val remainingRouteReversed = this.subList(0, i).reversed()
            val nextAltitudeConstraint = star?.let {
                remainingRouteReversed.routeToNextAltitudeConstraint(star.fixes).lastOrNull()?.let { fix ->
                    starMap?.get(fix.id)?.typicalAltitude
                }
            }

            val routeToNextSpeedConstraint = star?.let {
                remainingRouteReversed.routeToNextSpeedConstraint(it.fixes)
            }


            val nextSpeedConstraint =
                if (laterPoint.id == landingAirportIcao)
                    aircraftPerformance.landingVat
                else
                    routeToNextSpeedConstraint?.lastOrNull()?.let { fix ->
                        starMap?.get(fix.id)?.typicalSpeedIas
                    }

            val descentSteps = aircraftPerformance.computeDescentPathBackward(
                lowerAltitude = probeAltitude,
                higherAltitude = nextAltitudeConstraint ?: aircraftPosition.altitudeFt,
                laterPoint = probePosition,
                earlierPoint = earlierPoint.position,
                weatherData = verticalWeatherProfile,
                nextSpeedConstraint = nextSpeedConstraint,
                aircraftGroundSpeed = aircraftPosition.groundspeedKts,
            )

            descentSteps.forEach { step ->
                val stepLength = probePosition.distanceTo(step.position).toFloat()
                probingDistance += stepLength
                accumulatedTimeFromDestination += (stepLength / step.groundSpeed * 3600.0).roundToInt().seconds

                estimatedProfilePoints.add(
                    EstimatedProfilePoint(
                        inbound = laterPoint.id,
                        position = step.position,
                        altitude = step.altitudeFt,
                        remainingDistance = probingDistance,
                        remainingTime = accumulatedTimeFromDestination,
                        groundSpeed = step.groundSpeed,
                        tas = step.tas,
                        wind = step.wind,
                        heading = step.position.bearingTo(probePosition),
                        ias = step.ias
                    )
                )

                probeAltitude = step.altitudeFt
                probePosition = step.position
            }

            // We have reached cruise
            if (descentSteps.isEmpty()) {
                val weather = verticalWeatherProfile?.interpolateWeatherAtAltitude(probeAltitude)
                val bearing = probePosition.bearingTo(this.first().position)
                val expectedIas = aircraftPerformance.getPreferredIas(probeAltitude, weather?.temperatureC)
                val expectedTas = iasToTas(
                    expectedIas,
                    probeAltitude,
                    tempCelsius = weather?.temperatureC ?: getStandardTemperaturAt(probeAltitude)
                )

                estimatedProfilePoints.add(
                    EstimatedProfilePoint(
                        inbound = laterPoint.id,
                        position = earlierPoint.position,
                        altitude = probeAltitude,
                        remainingDistance = probingDistance,
                        remainingTime = accumulatedTimeFromDestination,
                        groundSpeed = tasToGs(expectedTas, weather?.wind ?: defaultWind, bearing),
                        tas = expectedTas,
                        wind = weather?.wind ?: defaultWind,
                        heading = bearing,
                        ias = expectedIas
                    )
                )
            }
        }


        return estimatedProfilePoints.reversed()
    }

    /**
     * Computes the descent path backward from a given altitude to a target altitude.
     *
     * @param lowerAltitude The altitude we are descending to
     * @param higherAltitude The target altitude we want to reach
     * @param laterPoint The point we are descending towards
     * @param earlierPoint The point we are descending from
     * @param weatherData The weather data at the current altitude
     */
    private fun AircraftPerformance.computeDescentPathBackward(
        lowerAltitude: Int,
        higherAltitude: Int,
        laterPoint: LatLng,
        earlierPoint: LatLng,
        weatherData: VerticalWeatherProfile?,
        nextSpeedConstraint: Int?,
        aircraftGroundSpeed: Int,
    ): List<DescentStep> {
        val descentPath = mutableListOf<DescentStep>()
        var probeAltitude = lowerAltitude
        var probePosition = laterPoint

        val deltaTime = 10.seconds
        val descentRateFpm = this.estimateDescentRate(probeAltitude)
        val verticalSpeed = descentRateFpm / 60.0 // ft/sec

        var remainingProbingDistance = laterPoint.distanceTo(earlierPoint)
        val estimatedOutsideTemperature = getStandardTemperaturAt(probeAltitude)

        while (true) {
            val probeWeather = weatherData?.interpolateWeatherAtAltitude(probeAltitude)

            val expectedIas = nextSpeedConstraint ?: getPreferredIas(probeAltitude, probeWeather?.temperatureC)
            val stepTas = iasToTas(expectedIas, probeAltitude, probeWeather?.temperatureC ?: estimatedOutsideTemperature)

            val stepGroundspeedKts = tasToGs(stepTas, probeWeather?.wind ?: defaultWind, earlierPoint.bearingTo(probePosition))
            val stepDistanceNm = (stepGroundspeedKts * deltaTime.inWholeSeconds) / 3600.0

            val newAltitude = probeAltitude + (verticalSpeed * deltaTime.inWholeSeconds).toInt()
            val reachingTargetAltitude = newAltitude > higherAltitude
            val reachingTargetPoint = remainingProbingDistance - stepDistanceNm < 0

            probeAltitude =
                if (reachingTargetAltitude)
                    higherAltitude
                else
                    newAltitude

            probePosition =
                if (reachingTargetPoint)
                    earlierPoint
                else
                    probePosition.interpolatePositionAlongPath(earlierPoint, stepDistanceNm)

            remainingProbingDistance -=
                if (reachingTargetPoint)
                    probePosition.distanceTo(earlierPoint)
                else
                    stepDistanceNm

            descentPath.add(
                DescentStep(
                    position = probePosition,
                    altitudeFt = probeAltitude,
                    groundSpeed = stepGroundspeedKts,
                    tas = stepTas,
                    wind = probeWeather?.wind ?: defaultWind,
                    ias = expectedIas
                )
            )

            if (reachingTargetPoint) {
                break
            }
        }

        return descentPath
    }

    private fun AircraftPerformance.estimateDescentRate(altitudeFt: Int): Int {
        // Example: https://contentzone.eurocontrol.int/aircraftperformance/details.aspx?ICAO=A321
        val vsAbove24k = initialDescentROD ?: descentROD ?: approachROD ?: 1000
        val vs24kTo10k = descentROD ?: approachROD ?: initialDescentROD ?: 1000
        val vs10kToGnd = approachROD ?: descentROD ?: initialDescentROD ?: 1000

        // Interpolate between the values based on altitude
        return when {
            altitudeFt < 10_000 -> {
                vs10kToGnd
            }
            altitudeFt in 10_000 until 24_000 -> {
                val ratio = (altitudeFt - 10_000).toDouble() / (24_000 - 10_000)
                ((1 - ratio) * vs10kToGnd + ratio * vs24kTo10k).toInt()
            }
            else -> {
                val ratio = (altitudeFt - 24_000).coerceAtMost(10_000).toDouble() / 10_000
                ((1 - ratio) * vs24kTo10k + ratio * vsAbove24k).toInt()
            }
        }
    }

    private fun AircraftPerformance.getPreferredIas(altitudeFt: Int, temperatureC: Int?): Int {
        // Define key IAS values
        val ias10kTo5k = approachIAS  // Assuming approach IAS value
        val ias24kTo10k = descentIAS  // Assuming descent IAS value
        val iasAbove24k = initialDescentMACH?.let {
            machToIAS(it, altitudeFt, temperatureC ?: getStandardTemperaturAt(altitudeFt))
        } ?: descentIAS

        // Interpolation for 10k to ground
        return when {
            altitudeFt < 5000 -> {
                // Interpolation between ias10kTo5k and landingVAT (5k to ground)
                val ratio = (altitudeFt / 5000.0)
                val iasAtAltitude = (1 - ratio) * ias10kTo5k + ratio * landingVat
                iasAtAltitude.toInt()
            }
            altitudeFt in 5000 until 10000 -> {
                // Interpolate between ias10kToGnd and ias24kTo10k (approach section)
                val ratio = (altitudeFt - 5000) / 5000.0
                val iasAtAltitude = (1 - ratio) * ias10kTo5k + ratio * ias24kTo10k
                iasAtAltitude.toInt()
            }
            altitudeFt in 10000 until 24000 -> {
                // Linear interpolation from ias10kToGnd to ias24kTo10k
                val ratio = (altitudeFt - 10000) / 14000.0
                val iasAtAltitude = (1 - ratio) * ias24kTo10k + ratio * iasAbove24k
                iasAtAltitude.toInt()
            }
            else -> {
                iasAbove24k
            }
        }
    }

    private fun List<RoutePoint>.routeToNextAltitudeConstraint(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.typicalAltitude != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

    private fun List<RoutePoint>.routeToNextSpeedConstraint(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.typicalSpeedIas != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

    private fun Star.findFinalAltitude(): Int =
        this.fixes.reversed().first {
            it.typicalAltitude != null
        }.let { fix ->
            fix.typicalAltitude ?: 0
        }
}
