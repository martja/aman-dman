package org.example.service

import org.example.model.entities.estimation.DescentSegment
import org.example.model.entities.estimation.DescentStep
import org.example.model.entities.navdata.LatLng
import org.example.model.entities.navigation.star.StarFix
import org.example.model.entities.navigation.AircraftPosition
import org.example.model.entities.navigation.RoutePoint
import org.example.model.entities.navigation.star.Constraint
import org.example.model.entities.navigation.star.Star
import org.example.model.entities.performance.AircraftPerformance
import org.example.model.entities.weather.VerticalWeatherProfile
import org.example.util.AircraftUtils.iasToTas
import org.example.util.AircraftUtils.tasToGs
import org.example.util.NavigationUtils.interpolatePositionAlongPath
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

object DescentProfileService {

    fun List<RoutePoint>.generateDescentSegments(
        aircraftPosition: AircraftPosition,
        verticalWeatherProfile: VerticalWeatherProfile,
        star: Star,
        aircraftPerformance: AircraftPerformance
    ): List<DescentSegment> {
        val starMap = star.fixes.associateBy { it.id }
        val descentSegments = mutableListOf<DescentSegment>()

        // Starts at the airports and works backwards
        var currentPosition = this.last().position
        var currentAltitude = star.airfieldElevationFt
        var remainingDistance = 0f
        var remainingTime = 0.seconds

        descentSegments.add(
            DescentSegment(
                inbound = this.last().id,
                position = currentPosition,
                targetAltitude = currentAltitude,
                remainingDistance = remainingDistance,
                remainingTime = remainingTime,
                groundSpeed = 0,
                tas = 0
            )
        )

        // Start from the last waypoint and work backward
        for (i in lastIndex downTo 1) {
            val fromWaypoint = this[i]
            val targetWaypoint = this[i-1]
            val remainingRouteReversed = this.subList(0, i).reversed()
            val nextAltitudeConstraint = remainingRouteReversed.routeToNextAltitudeConstraint(star.fixes).lastOrNull()?.let { fix ->
                starMap[fix.id]?.starAltitudeConstraint
            } ?: Constraint.Exact(aircraftPosition.altitudeFt)

            val descentPath = aircraftPerformance.computeDescentPathBackward(
                nextAltitudeConstraint = nextAltitudeConstraint,
                fromPoint = currentPosition,
                toPoint = targetWaypoint.position,
                weatherData = verticalWeatherProfile,
                airfieldAltitude = star.airfieldElevationFt,
                fromAltFt = currentAltitude
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
                    )
                )

                currentAltitude = step.altitudeFt
                currentPosition = step.position
            }
        }
        return descentSegments.reversed()
    }

    private fun AircraftPerformance.computeDescentPathBackward(
        fromAltFt: Int,
        nextAltitudeConstraint: Constraint,
        fromPoint: LatLng,
        toPoint: LatLng,
        weatherData: VerticalWeatherProfile,
        airfieldAltitude: Int,
    ): List<DescentStep> {
        val descentPath = mutableListOf<DescentStep>()
        var currentAltitude = fromAltFt
        var currentPosition = fromPoint

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
            val stepWeather = weatherData.interpolateWeatherAtAltitude(currentAltitude)
            val expectedIas = this.getPreferredSpeed(currentAltitude, airfieldAltitude)
            val stepTas = iasToTas(expectedIas, currentAltitude, stepWeather.temperatureC)
            val stepGroundspeedKts = tasToGs(stepTas, stepWeather.wind, currentPosition.bearingTo(toPoint))
            val stepDistanceNm = (stepGroundspeedKts * deltaTime) / 3600.0

            val newAltitude = currentAltitude + (verticalSpeed * deltaTime).toInt()
            val newPosition = currentPosition.interpolatePositionAlongPath(toPoint, stepDistanceNm)

            if (newAltitude > maxAltFt || remainingDistance - stepDistanceNm < 0) {
                // We have reached the target altitude or the target point
                descentPath.add(
                    DescentStep(
                        position = toPoint,
                        altitudeFt = max(minAltFt, min(newAltitude, maxAltFt)),
                        groundSpeed = stepGroundspeedKts,
                        tas = stepTas
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
                    tas = stepTas
                )
            )

        }

        return descentPath
    }

    private fun AircraftPerformance.estimateDescentRate(altitudeFt: Int) =
        if (altitudeFt < 10_000) {
            this.approachROD ?: this.descentROD ?: this.initialDescentROD ?: 1000
        } else {
            this.descentROD ?: this.initialDescentROD ?: this.approachROD ?: 1000
        }

    private fun AircraftPerformance.getPreferredSpeed(altitudeFt: Int, airfieldAltitude: Int) =
        if (altitudeFt < airfieldAltitude + 3000) {
            this.landingVat
        } else if (altitudeFt < 10_000) {
            this.approachIAS
        } else {
            this.descentIAS
        }

    private fun List<RoutePoint>.routeToNextAltitudeConstraint(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.starAltitudeConstraint != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }
}
