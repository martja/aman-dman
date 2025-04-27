package org.example

import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.Star
import org.example.entities.navigation.star.StarFix
import org.example.util.PhysicsUtils.iasToTas
import org.example.util.PhysicsUtils.tasToGs
import org.example.util.NavigationUtils.interpolatePositionAlongPath
import org.example.util.PhysicsUtils
import org.example.util.PhysicsUtils.machToIAS
import org.example.util.WeatherUtils.getStandardTemperatureAt
import org.example.util.WeatherUtils.interpolateWeatherAtAltitude
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

object DescentTrajectoryService {

    private const val CURRENT_ID = "CURRENT"
    private const val DECELERATION_RATE = 0.5
    private val calmWind = Wind(0, 0)

    fun List<RoutePoint>.calculateDescentTrajectory(
        aircraftPosition: AircraftPosition,
        verticalWeatherProfile: VerticalWeatherProfile?,
        star: Star?,
        aircraftPerformance: AircraftPerformance,
        landingAirportIcao: String,
        flightPlanTas: Int?
    ): List<TrajectoryPoint> {
        val starMap = star?.fixes?.associateBy { it.id }
        val trajectoryPoints = mutableListOf<TrajectoryPoint>()
        val lastAltitudeConstraint = star?.findFinalAltitude()

        // Starts at the airports and works backwards
        var probePosition = this.last().position
        var probeAltitude = lastAltitudeConstraint ?: 0
        var probingDistance = 0f
        var accumulatedTimeFromDestination = 0.seconds

        val remainingRoute =
            listOf(RoutePoint(id = CURRENT_ID, position = aircraftPosition.position, isOnStar = false, isPassed = false)) + this.filter { !it.isPassed }

        // Add the last point (the airport) to the profile
        trajectoryPoints +=
            TrajectoryPoint(
                position = probePosition,
                altitude = probeAltitude,
                remainingDistance = probingDistance,
                remainingTime = accumulatedTimeFromDestination,
                groundSpeed = aircraftPerformance.landingVat, // TODO: convert to ground speed
                tas = aircraftPerformance.landingVat,
                wind = calmWind, // TODO: use wind from METAR
                ias = aircraftPerformance.landingVat,
                heading = this.getFinalHeading() ?: aircraftPosition.trackDeg,
                fixId = landingAirportIcao
            )

        // Start from the last waypoint (the airport) and work backward
        for (i in remainingRoute.lastIndex downTo 1) {
            val laterPoint = remainingRoute[i]
            val earlierPoint = remainingRoute[i-1]
            val remainingRouteReversed = remainingRoute.subList(0, i).reversed()

            val nextAltitudeExpectation = star?.nextAltitudeExpectation(remainingRouteReversed)
                ?.let { starMap?.get(it.id)?.typicalAltitude }
                ?: aircraftPosition.altitudeFt

            val earlierSpeedExpectation = star?.let {
                getInterpolatedSpeedExpectation(star = it.fixes, atRoutePoint = earlierPoint)
            } ?: aircraftPerformance.getPreferredIas(
                altitudeFt = nextAltitudeExpectation,
                temperatureC = verticalWeatherProfile?.interpolateWeatherAtAltitude(nextAltitudeExpectation)?.temperatureC,
                flightPlanTas = flightPlanTas
            )

            val descentSteps = aircraftPerformance.computeDescentPathBackward(
                lowerAltitude = probeAltitude,
                higherAltitude = nextAltitudeExpectation,
                laterPoint = probePosition,
                earlierPoint = earlierPoint.position,
                verticalWeatherProfile = verticalWeatherProfile,
                earlierExpectedSpeed = earlierSpeedExpectation,
                laterExpectedSpeed = trajectoryPoints.last().ias,
                flightPlanTas = flightPlanTas
            )

            for (step in descentSteps) {
                val isLastStep = step == descentSteps.last()
                val stepLength = probePosition.distanceTo(step.position).toFloat()
                probingDistance += stepLength
                accumulatedTimeFromDestination += (stepLength / step.groundSpeed * 3600.0).roundToInt().seconds

                trajectoryPoints +=
                    TrajectoryPoint(
                        position = step.position,
                        altitude = step.altitudeFt,
                        remainingDistance = probingDistance,
                        remainingTime = accumulatedTimeFromDestination,
                        groundSpeed = step.groundSpeed,
                        tas = step.tas,
                        wind = step.wind,
                        heading = step.position.bearingTo(probePosition),
                        ias = step.ias,
                        fixId = if (isLastStep && earlierPoint.id != CURRENT_ID) earlierPoint.id else null
                    )

                probeAltitude = step.altitudeFt
                probePosition = step.position
            }
        }

        return trajectoryPoints.reversed()
    }

    /**
     * Computes the descent path backward from a given altitude to a target altitude.
     *
     * @param lowerAltitude The altitude we are descending to
     * @param higherAltitude The target altitude we want to reach
     * @param laterPoint The point we are descending towards
     * @param earlierPoint The point we are descending from
     * @param verticalWeatherProfile The weather data above the airport
     */
    private fun AircraftPerformance.computeDescentPathBackward(
        lowerAltitude: Int,
        higherAltitude: Int,
        laterPoint: LatLng,
        earlierPoint: LatLng,
        verticalWeatherProfile: VerticalWeatherProfile?,
        earlierExpectedSpeed: Int?,
        laterExpectedSpeed: Int?,
        flightPlanTas: Int?
    ): List<DescentStep> {
        val descentPath = mutableListOf<DescentStep>()
        var probeAltitude = lowerAltitude
        var probePosition = laterPoint

        val deltaTime = 10.seconds
        val descentRateFpm = this.estimateDescentRate(probeAltitude)
        val verticalSpeed = descentRateFpm / 60.0 // ft/sec

        var remainingProbingDistance = laterPoint.distanceTo(earlierPoint)
        var currentExpectedSpeed = laterExpectedSpeed ?: this.getPreferredIas(probeAltitude, verticalWeatherProfile?.interpolateWeatherAtAltitude(probeAltitude)?.temperatureC, flightPlanTas)

        while (true) {
            val probeWeather = verticalWeatherProfile?.interpolateWeatherAtAltitude(probeAltitude)
            val estimatedOutsideTemperature = getStandardTemperatureAt(probeAltitude)

            val targetSpeed = earlierExpectedSpeed ?: getPreferredIas(probeAltitude, probeWeather?.temperatureC, flightPlanTas)
            val maxSpeedChange = deltaTime.inWholeSeconds.toInt() * DECELERATION_RATE
            val speedDifference = (targetSpeed - currentExpectedSpeed).toDouble()
            val speedAdjustment = speedDifference.coerceIn(-maxSpeedChange, maxSpeedChange)

            currentExpectedSpeed += speedAdjustment.roundToInt()

            val stepTas = iasToTas(currentExpectedSpeed, probeAltitude, probeWeather?.temperatureC ?: estimatedOutsideTemperature)
            val stepGroundSpeedKts = tasToGs(stepTas, probeWeather?.wind ?: calmWind, earlierPoint.bearingTo(probePosition))
            val stepDistanceNm = (stepGroundSpeedKts * deltaTime.inWholeSeconds) / 3600.0

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
                    groundSpeed = stepGroundSpeedKts,
                    tas = stepTas,
                    wind = probeWeather?.wind ?: calmWind,
                    ias = currentExpectedSpeed
                )
            )

            if (reachingTargetPoint) {
                break
            }
        }

        return descentPath
    }

    /**
     * Interpolate typical speed for a STAR fix that doesn't have a typical speed, but is between two fixes that do.
     */
    private fun List<RoutePoint>.getInterpolatedSpeedExpectation(star: List<StarFix>, atRoutePoint: RoutePoint): Int? {
        val laterSpeedRestriction = this.nextSpeedExpectation(atRoutePoint, star)
        val priorSpeedRestriction = this.previousSpeedExpectation(atRoutePoint, star)

        if (laterSpeedRestriction == null) {
            return priorSpeedRestriction?.first?.typicalSpeedIas
        }

        if (priorSpeedRestriction == null) {
            return null
        }

        val distanceToSpeedExpectation = distanceBetweenPoints(atRoutePoint, laterSpeedRestriction.second)
        val distanceToSpeedExpectationBehind = distanceBetweenPoints(atRoutePoint, priorSpeedRestriction.second)

        // Interpolate
        val ratio = distanceToSpeedExpectation / (distanceToSpeedExpectation + distanceToSpeedExpectationBehind)
        val speedAhead = laterSpeedRestriction.first.typicalSpeedIas ?: return null
        val speedBehind = priorSpeedRestriction.first.typicalSpeedIas ?: return null
        return (speedBehind * ratio + speedAhead * (1 - ratio)).toInt()
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

    private fun AircraftPerformance.getPreferredIas(altitudeFt: Int, temperatureC: Int?, flightPlanTas: Int?): Int {
        val tempOrStandardTemp = temperatureC ?: getStandardTemperatureAt(altitudeFt)

        val machIas =
            flightPlanTas?.let { PhysicsUtils.tasToIAS(it, altitudeFt, tempOrStandardTemp) }
                ?: initialDescentMACH?.let { machToIAS(it, altitudeFt, tempOrStandardTemp) }
                ?: descentIAS

        return when {
            altitudeFt > 30000 -> {
                // High cruise descent: Mach hold
                machIas
            }
            altitudeFt in 28000..30000 -> {
                // Blend Mach to IAS between FL280-FL300
                val ratio = (30000 - altitudeFt) / 2000.0
                val blendedIas = machIas * ratio + descentIAS * (1 - ratio)
                blendedIas.toInt()
            }
            altitudeFt in 10000..28000 -> {
                // Constant descent IAS below Mach hold
                descentIAS
            }
            altitudeFt in 5000 until 10000 -> {
                // From 10k to 5k, interpolate approach IAS from descent IAS
                val ratio = (altitudeFt - 5000) / 5000.0
                val interpolatedIas = (1 - ratio) * approachIAS + ratio * descentIAS
                interpolatedIas.coerceAtMost(240.0).toInt() // Cap at 250 kt
            }
            else -> {
                // From 5k to ground, interpolate landing speed
                val ratio = (altitudeFt / 5000.0)
                val interpolatedIas = (1 - ratio) * landingVat + ratio * approachIAS
                interpolatedIas.coerceAtMost(240.0).toInt() // Still cap at 250 kt
            }
        }
    }

    private fun List<RoutePoint>.nextSpeedExpectation(atRoutePoint: RoutePoint, star: List<StarFix>): Pair<StarFix, RoutePoint>? {
        val currentPointIndex = this.indexOf(atRoutePoint)
        if (currentPointIndex == -1) return null

        for (i in currentPointIndex until this.size) {
            val routePoint = this[i]
            val starFix = star.find { it.id == routePoint.id }
            if (starFix?.typicalSpeedIas != null) {
                return Pair(starFix, routePoint)
            }
        }
        return null
    }

    private fun List<RoutePoint>.previousSpeedExpectation(atRoutePoint: RoutePoint, star: List<StarFix>): Pair<StarFix, RoutePoint>? {
        val currentPointIndex = this.indexOf(atRoutePoint)
        if (currentPointIndex == -1) return null

        for (i in currentPointIndex - 1 downTo 0) {
            val routePoint = this[i]
            val starFix = star.find { it.id == routePoint.id }
            if (starFix?.typicalSpeedIas != null) {
                return Pair(starFix, routePoint)
            }
        }
        return null
    }

    private fun List<RoutePoint>.distanceBetweenPoints(fromPoint: RoutePoint, toRoutePoint: RoutePoint): Double {
        val fromIndex = this.indexOf(fromPoint)
        val toIndex = this.indexOf(toRoutePoint)

        val subList =
            if (fromIndex > toIndex) {
                this.subList(toIndex, fromIndex + 1)
            } else {
                this.subList(fromIndex, toIndex + 1)
            }

       return subList
            .map { it.position }
            .zipWithNext()
            .sumOf { (from, to) -> from.distanceTo(to) }
    }

    private fun List<RoutePoint>.routeToNextAltitudeExpectation(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.typicalAltitude != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

    private fun List<RoutePoint>.routeToNextSpeedExpectation(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.typicalSpeedIas != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

    private fun List<RoutePoint>.totalLength() =
        this.sumOf { it.position.distanceTo(this.last().position) }

    private fun Star.nextAltitudeExpectation(route: List<RoutePoint>): RoutePoint? =
        route.routeToNextAltitudeExpectation(fixes).lastOrNull()

    private fun Star.nextSpeedConstraint(route: List<RoutePoint>): RoutePoint? =
        route.routeToNextSpeedExpectation(fixes).lastOrNull()

    private fun List<RoutePoint>.getFinalHeading(): Int? =
        if (this.size >= 2)
            this[this.lastIndex - 1].position.bearingTo(this.last().position)
        else null

    private fun Star.findFinalAltitude(): Int =
        this.fixes.reversed().first {
            it.typicalAltitude != null
        }.let { fix ->
            fix.typicalAltitude ?: 0
        }
}
