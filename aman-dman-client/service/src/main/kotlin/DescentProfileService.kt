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

    private val calmWind = Wind(0, 0)

    fun List<RoutePoint>.generateDescentSegments(
        aircraftPosition: AircraftPosition,
        verticalWeatherProfile: VerticalWeatherProfile?,
        star: Star?,
        aircraftPerformance: AircraftPerformance,
        landingAirportIcao: String
    ): List<ProfilePointEstimation> {
        val starMap = star?.fixes?.associateBy { it.id }
        val profilePointEstimations = mutableListOf<ProfilePointEstimation>()
        val lastAltitudeConstraint = star?.findFinalAltitude()

        // Starts at the airports and works backwards
        var probePosition = this.last().position
        var probeAltitude = lastAltitudeConstraint ?: 0
        var probingDistance = 0f
        var accumulatedTimeFromDestination = 0.seconds

        // Add the last point (the airport) to the profile
        profilePointEstimations +=
            ProfilePointEstimation(
                inbound = this.last().id,
                position = probePosition,
                altitude = probeAltitude,
                remainingDistance = probingDistance,
                remainingTime = accumulatedTimeFromDestination,
                groundSpeed = aircraftPerformance.landingVat, // TODO: convert to ground speed
                tas = aircraftPerformance.landingVat,
                wind = calmWind, // TODO: use wind from METAR
                ias = aircraftPerformance.landingVat,
                heading = this.getFinalHeading() ?: aircraftPosition.trackDeg
            )

        // Start from the last waypoint (the airport) and work backward
        for (i in lastIndex downTo 1) {
            val laterPoint = this[i]
            val earlierPoint = this[i-1]
            val remainingRouteReversed = this.subList(0, i).reversed()

            val nextAltitudeExpectation = star?.nextAltitudeExpectation(remainingRouteReversed)
                ?.let { starMap?.get(it.id)?.typicalAltitude }
                ?: aircraftPosition.altitudeFt

            val nextSpeed =
                when (laterPoint.id) {
                    landingAirportIcao ->
                        aircraftPerformance.landingVat
                    else ->
                        star?.nextSpeedConstraint(remainingRouteReversed)
                            ?.let { starMap?.get(it.id)?.typicalSpeedIas }
                }

            val descentSteps = aircraftPerformance.computeDescentPathBackward(
                lowerAltitude = probeAltitude,
                higherAltitude = nextAltitudeExpectation,
                laterPoint = probePosition,
                earlierPoint = earlierPoint.position,
                verticalWeatherProfile = verticalWeatherProfile,
                laterExpectedSpeed = nextSpeed,
            )

            for (step in descentSteps) {
                val stepLength = probePosition.distanceTo(step.position).toFloat()
                probingDistance += stepLength
                accumulatedTimeFromDestination += (stepLength / step.groundSpeed * 3600.0).roundToInt().seconds

                profilePointEstimations +=
                    ProfilePointEstimation(
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

                probeAltitude = step.altitudeFt
                probePosition = step.position
            }
        }

        return profilePointEstimations.reversed()
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
        laterExpectedSpeed: Int?,
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
            val probeWeather = verticalWeatherProfile?.interpolateWeatherAtAltitude(probeAltitude)

            val expectedIas = laterExpectedSpeed ?: getPreferredIas(probeAltitude, probeWeather?.temperatureC)
            val stepTas = iasToTas(expectedIas, probeAltitude, probeWeather?.temperatureC ?: estimatedOutsideTemperature)

            val stepGroundspeedKts = tasToGs(stepTas, probeWeather?.wind ?: calmWind, earlierPoint.bearingTo(probePosition))
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
                    wind = probeWeather?.wind ?: calmWind,
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
        val standardTemp = temperatureC ?: getStandardTemperaturAt(altitudeFt)

        val machIas = initialDescentMACH?.let {
            machToIAS(it, altitudeFt, standardTemp)
        } ?: descentIAS

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



    private fun List<RoutePoint>.routeToNextAltitudeExpectation(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.typicalAltitude != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

    private fun List<RoutePoint>.routeToNextSpeedExpectation(star: List<StarFix>): List<RoutePoint> {
        val i = this.indexOfFirst { star.any { fix -> fix.id == it.id && fix.typicalSpeedIas != null } }
        return if (i == -1) emptyList() else this.subList(0, i + 1)
    }

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
