package org.example

import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.Star
import org.example.entities.navigation.star.StarFix
import org.example.util.PhysicsUtils.gsToTas
import org.example.util.PhysicsUtils.iasToTas
import org.example.util.PhysicsUtils.tasToGs
import org.example.util.NavigationUtils.interpolatePositionAlongPath
import org.example.util.PhysicsUtils.machToIAS
import org.example.util.WeatherUtils.getStandardTemperaturAt
import org.example.util.WeatherUtils.interpolateWeatherAtAltitude
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

object DescentProfileService {

    val defaultWind = Wind(0, 0)

    fun List<RoutePoint>.generateDescentSegments(
        aircraftPosition: AircraftPosition,
        verticalWeatherProfile: VerticalWeatherProfile?,
        star: Star?,
        aircraftPerformance: AircraftPerformance
    ): List<DescentSegment> {
        val starMap = star?.fixes?.associateBy { it.id }
        val descentSegments = mutableListOf<DescentSegment>()
        val lastAltitudeConstraint = star?.findFinalAltitude()

        // Starts at the airports and works backwards
        var currentPosition = this.last().position
        var currentAltitude = lastAltitudeConstraint ?: 0
        var remainingDistance = 0f
        var remainingTime = 0.seconds

        descentSegments.add(
            DescentSegment(
                inbound = this.last().id,
                position = currentPosition,
                targetAltitude = currentAltitude,
                remainingDistance = remainingDistance,
                remainingTime = remainingTime,
                groundSpeed = aircraftPerformance.landingVat, // TODO: apply wind
                tas = aircraftPerformance.landingVat,
                wind = defaultWind,
                heading =
                    if (this.size >= 2)
                        this[lastIndex - 1].position.bearingTo(this[lastIndex].position)
                    else 0
            )
        )

        // Start from the last waypoint (the airport) and work backward
        for (i in lastIndex downTo 1) {
            val fromWaypoint = this[i]
            val targetWaypoint = this[i-1]
            val remainingRouteReversed = this.subList(0, i).reversed()
            val nextAltitudeConstraint = star?.let {
                remainingRouteReversed.routeToNextAltitudeConstraint(star.fixes).lastOrNull()?.let { fix ->
                    starMap?.get(fix.id)?.typicalAltitude
                }
            }

            val routeToNextSpeedConstraint = star?.let {
                remainingRouteReversed.routeToNextSpeedConstraint(it.fixes)
            }

            val nextSpeedConstraint = routeToNextSpeedConstraint?.lastOrNull()?.let { fix ->
                starMap?.get(fix.id)?.typicalSpeedIas
            }

            val distanceToNextSpeedConstraint = routeToNextSpeedConstraint?.zipWithNext { a, b -> a.position.distanceTo(b.position) }?.sum()


            val descentSteps = aircraftPerformance.computeDescentPathBackward(
                lowerAltitude = currentAltitude,
                higherAltitude = nextAltitudeConstraint ?: aircraftPosition.altitudeFt,
                lowerPoint = currentPosition,
                higherPoint = targetWaypoint.position,
                weatherData = verticalWeatherProfile,
                distanceToNextSpeedConstraint = distanceToNextSpeedConstraint?.roundToInt(),
                nextSpeedConstraint = nextSpeedConstraint,
                aircraftAltitude = aircraftPosition.altitudeFt,
                aircraftGroundSpeed = aircraftPosition.groundspeedKts,
            )

            descentSteps.forEach { step ->
                val stepLength = currentPosition.distanceTo(step.position).toFloat()
                remainingDistance += stepLength
                remainingTime += (stepLength / step.groundSpeed * 3600.0).roundToInt().seconds

                descentSegments.add(
                    DescentSegment(
                        inbound = fromWaypoint.id,
                        position = step.position,
                        targetAltitude = step.altitudeFt,
                        remainingDistance = remainingDistance,
                        remainingTime = remainingTime,
                        groundSpeed = step.groundSpeed,
                        tas = step.tas,
                        wind = step.wind,
                        heading = step.position.bearingTo(currentPosition)
                    )
                )

                currentAltitude = step.altitudeFt
                currentPosition = step.position
            }

            // We have reached cruise
            if (descentSteps.isEmpty()) {
                val weather = verticalWeatherProfile?.interpolateWeatherAtAltitude(currentAltitude)
                val bearing = currentPosition.bearingTo(this.first().position)
                val expectedTas = iasToTas(
                    aircraftPerformance.getPreferredIas(currentAltitude, weather?.temperatureC),
                    currentAltitude,
                    tempCelsius = weather?.temperatureC ?: getStandardTemperaturAt(currentAltitude)
                )

                descentSegments.add(
                    DescentSegment(
                        inbound = fromWaypoint.id,
                        position = targetWaypoint.position,
                        targetAltitude = currentAltitude,
                        remainingDistance = remainingDistance,
                        remainingTime = remainingTime,
                        groundSpeed = tasToGs(expectedTas, weather?.wind ?: defaultWind, bearing),
                        tas = expectedTas,
                        wind = weather?.wind ?: defaultWind,
                        heading = bearing
                    )
                )
            }
        }


        return descentSegments.reversed()
    }

    /**
     * Computes the descent path backward from a given altitude to a target altitude.
     *
     * @param lowerAltitude The altitude we are descending to
     * @param higherAltitude The target altitude we want to reach
     * @param lowerPoint The point we are descending towards
     * @param higherPoint The point we are descending from
     * @param weatherData The weather data at the current altitude
     */
    private fun AircraftPerformance.computeDescentPathBackward(
        lowerAltitude: Int,
        higherAltitude: Int,
        lowerPoint: LatLng,
        higherPoint: LatLng,
        weatherData: VerticalWeatherProfile?,
        nextSpeedConstraint: Int?,
        aircraftAltitude: Int,
        aircraftGroundSpeed: Int,
        distanceToNextSpeedConstraint: Int?,
    ): List<DescentStep> {
        val descentPath = mutableListOf<DescentStep>()
        var currentAltitude = lowerAltitude
        var currentPosition = lowerPoint

        val aircraftWind = weatherData?.interpolateWeatherAtAltitude(currentAltitude)?.wind
        val currentTas = gsToTas(aircraftGroundSpeed, aircraftWind ?: defaultWind, currentPosition.bearingTo(higherPoint))

        val deltaTime = 10.0 // seconds
        val descentRateFpm = this.estimateDescentRate(currentAltitude)
        val verticalSpeed = descentRateFpm / 60.0 // ft/sec

        var remainingDistance = lowerPoint.distanceTo(higherPoint)
        val estimatedOutsideTemperature = getStandardTemperaturAt(currentAltitude)

        while (true) {
            val stepWeather = weatherData?.interpolateWeatherAtAltitude(currentAltitude)
            val stepTas = nextSpeedConstraint?.let {
                iasToTas(nextSpeedConstraint, currentAltitude, stepWeather?.temperatureC ?: estimatedOutsideTemperature)
            } ?: iasToTas(getPreferredIas(currentAltitude, stepWeather?.temperatureC), currentAltitude, stepWeather?.temperatureC ?: estimatedOutsideTemperature)

            val stepGroundspeedKts = tasToGs(stepTas, stepWeather?.wind ?: defaultWind, currentPosition.bearingTo(higherPoint))
            val stepDistanceNm = (stepGroundspeedKts * deltaTime) / 3600.0

            val newAltitude = currentAltitude + (verticalSpeed * deltaTime).toInt()
            val newPosition = currentPosition.interpolatePositionAlongPath(higherPoint, stepDistanceNm)

            if (newAltitude > higherAltitude || remainingDistance - stepDistanceNm < 0 || newAltitude > aircraftAltitude) {
                // We have reached the target altitude or the target point
                descentPath.add(
                    DescentStep(
                        position = higherPoint,
                        altitudeFt = max(lowerAltitude, min(newAltitude, higherAltitude)),
                        groundSpeed = stepGroundspeedKts,
                        tas = min(currentTas, stepTas),
                        wind = stepWeather?.wind ?: defaultWind
                    )
                )
                break
            }

            currentAltitude = newAltitude
            currentPosition = newPosition
            remainingDistance -= stepDistanceNm

            descentPath.add(
                DescentStep(
                    position = currentPosition,
                    altitudeFt = currentAltitude,
                    groundSpeed = stepGroundspeedKts,
                    tas = min(currentTas, stepTas),
                    wind = stepWeather?.wind ?: defaultWind
                )
            )

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
                val ratio = (altitudeFt - 24_000).coerceAtMost(10_000).toDouble() / 10_000  // cap interpolation up to 34k
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
