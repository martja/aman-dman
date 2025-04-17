package org.example

import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.Constraint
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

        // Start from the last waypoint and work backward
        for (i in lastIndex downTo 1) {
            val fromWaypoint = this[i]
            val targetWaypoint = this[i-1]
            val remainingRouteReversed = this.subList(0, i).reversed()
            val nextAltitudeConstraint = star?.let {
                remainingRouteReversed.routeToNextAltitudeConstraint(star.fixes).lastOrNull()?.let { fix ->
                    starMap?.get(fix.id)?.starAltitudeConstraint
                }
            }

            val descentPath = aircraftPerformance.computeDescentPathBackward(
                nextAltitudeConstraint = nextAltitudeConstraint ?: Constraint.Exact(aircraftPosition.altitudeFt),
                fromPoint = currentPosition,
                toPoint = targetWaypoint.position,
                weatherData = verticalWeatherProfile,
                lastAltitudeConstraint = lastAltitudeConstraint ?: 0,
                fromAltFt = currentAltitude,
                aircraftAltitude = aircraftPosition.altitudeFt,
                aircraftGroundSpeed = aircraftPosition.groundspeedKts,
            )

            descentPath.forEach { step ->
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
            if (descentPath.isEmpty()) {
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

    private fun AircraftPerformance.computeDescentPathBackward(
        fromAltFt: Int,
        nextAltitudeConstraint: Constraint,
        fromPoint: LatLng,
        toPoint: LatLng,
        weatherData: VerticalWeatherProfile?,
        lastAltitudeConstraint: Int,
        aircraftAltitude: Int,
        aircraftGroundSpeed: Int,
    ): List<DescentStep> {
        val descentPath = mutableListOf<DescentStep>()
        var currentAltitude = fromAltFt
        var currentPosition = fromPoint

        val aircraftWind = weatherData?.interpolateWeatherAtAltitude(currentAltitude)?.wind
        val currentTas = gsToTas(aircraftGroundSpeed, aircraftWind ?: defaultWind, currentPosition.bearingTo(toPoint))

        val deltaTime = 10.0 // seconds
        val descentRateFpm = this.estimateDescentRate(currentAltitude)
        val verticalSpeed = descentRateFpm / 60.0 // ft/sec

        var remainingDistance = fromPoint.distanceTo(toPoint)

        val minAltFt = when (nextAltitudeConstraint) {
            is Constraint.Min -> nextAltitudeConstraint.value
            is Constraint.Between -> nextAltitudeConstraint.min
            else -> Int.MIN_VALUE
        }

        val maxAltFt = when (nextAltitudeConstraint) {
            is Constraint.Max -> nextAltitudeConstraint.value
            is Constraint.Exact -> nextAltitudeConstraint.value
            is Constraint.Between -> nextAltitudeConstraint.max
            else -> Int.MAX_VALUE
        }

        while (true) {
            val stepWeather = weatherData?.interpolateWeatherAtAltitude(currentAltitude)
            val expectedIas = this.getPreferredIas(currentAltitude, stepWeather?.temperatureC)
            val stepTas = iasToTas(expectedIas, currentAltitude, stepWeather?.temperatureC ?: 0) // TODO: estimate
            val stepGroundspeedKts = tasToGs(stepTas, stepWeather?.wind ?: defaultWind, currentPosition.bearingTo(toPoint))
            val stepDistanceNm = (stepGroundspeedKts * deltaTime) / 3600.0

            val newAltitude = currentAltitude + (verticalSpeed * deltaTime).toInt()
            val newPosition = currentPosition.interpolatePositionAlongPath(toPoint, stepDistanceNm)

            if (newAltitude > maxAltFt || remainingDistance - stepDistanceNm < 0 || newAltitude > aircraftAltitude) {
                // We have reached the target altitude or the target point
                descentPath.add(
                    DescentStep(
                        position = toPoint,
                        altitudeFt = max(minAltFt, min(newAltitude, maxAltFt)),
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
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.starAltitudeConstraint != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

    private fun Star.findFinalAltitude(): Int =
        this.fixes.reversed().first {
            it.starAltitudeConstraint != null
        }.let { fix ->
            return when (val constraint = fix.starAltitudeConstraint) {
                is Constraint.Min -> constraint.value
                is Constraint.Max -> constraint.value
                is Constraint.Between -> constraint.max
                is Constraint.Exact -> constraint.value
                null -> 0
            }
        }
}
