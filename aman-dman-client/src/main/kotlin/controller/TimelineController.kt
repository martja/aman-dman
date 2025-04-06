package org.example.controller

import kotlinx.datetime.Instant
import model.entities.TimelineConfig
import org.example.config.AircraftPerformanceData
import org.example.integration.AtcClient
import org.example.integration.entities.ArrivalJson
import org.example.integration.entities.FixPointJson
import org.example.model.*
import org.example.model.entities.navdata.LatLng
import org.example.model.entities.navigation.AircraftPosition
import org.example.model.entities.navigation.RoutePoint
import org.example.model.entities.navigation.star.lunip4l
import org.example.model.entities.weather.Wind
import org.example.model.service.DescentProfileService.generateDescentSegments
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimelineController(
    private val timelineState: TimelineState,
    atcClient: AtcClient,
    timelineConfig: TimelineConfig,
    private val mainController: MainController
) {
    val minSeparationNauticalMiles = 3f
    val expectedSpeedOnFinalFix = 180f
    val minSeparation = (minSeparationNauticalMiles / expectedSpeedOnFinalFix * 60).roundToInt().minutes

    init {
        timelineState.addListener { event ->
            if (event.propertyName == "timeNow") {
                recalculateSequence(timelineState.timelineOccurrences)
            }
        }

        atcClient.collectArrivalsFor(
            timelineConfig.airports.first(),
            onDataReceived = this::handleArrivals
        )

        atcClient.collectDeparturesFrom(
            airportIcao = timelineConfig.airports.first(),
            onDataReceived = this::handleDepartures
        )
    }

    private fun handleArrivals(arrivals: List<ArrivalJson>) {
        timelineState.arrivalOccurrences = arrivals
            .map {
                val aircraftPerformance = AircraftPerformanceData.get(arrivals.first().icaoType)
                val remainingRoute =
                    listOf(RoutePoint(it.callsign, LatLng(it.latitude, it.longitude))) + it.remainingRoute.map { RoutePoint(it.name, LatLng(it.latitude, it.longitude)) }
                val descentSegments = remainingRoute.generateDescentSegments(
                    AircraftPosition(
                        position = LatLng(it.latitude, it.longitude),
                        altitudeFt = it.flightLevel,
                        groundspeedKts = it.groundSpeed,
                        trackDeg = it.track
                    ),
                    timelineState.verticalWeatherProfile,
                    lunip4l,
                    aircraftPerformance
                )

                if (it.callsign == "BAW5PT") {
                    mainController.openProfileWindow(descentSegments)
                }

                RunwayArrivalOccurrence(
                    timelineId = 0,
                    assignedStar = it.assignedStar,
                    trackingController = it.trackingController,
                    callsign = it.callsign,
                    icaoType = it.icaoType,
                    runway = it.assignedRunway,
                    time = timelineState.timeNow + descentSegments.first().remainingTime,
                    flightLevel = it.flightLevel,
                    pressureAltitude = it.pressureAltitude,
                    groundSpeed = it.groundSpeed,
                    wakeCategory = aircraftPerformance.takeOffWTC,
                    arrivalAirportIcao = it.arrivalAirportIcao,
                )
            }
    }

    private fun handleDepartures(departureOccurrences: List<DepartureOccurrence>) {
        timelineState.departureOccurrences = departureOccurrences
    }

    fun addDelayDefinition(name: String, from: Instant, duration: Duration, runway: String) {
        timelineState.addDelayDefinition(name, from, duration, runway)
    }

    private fun calculateExtraTimeForWind(fixInboundOccurrence: FixInboundOccurrence): Duration {
        var windDelayAcc = 0.seconds
        if(fixInboundOccurrence.callsign == "NSZ3220") {
            println(fixInboundOccurrence.descentProfile.joinToString {
                "[" + it.minAltitude.toString() + "-" + it.maxAltitude.toString() + "] hdg: " + it.averageHeading.toString() + " dur: " + it.duration.toString() + " dist: " + it.distance.toString()
            })
        }
        fixInboundOccurrence.descentProfile.forEach { sector ->
            val closestWindSegment = timelineState.verticalWeatherProfile.weatherLayers
                .sortedBy { it.flightLevelFt }
                .firstOrNull { it.flightLevelFt >= sector.minAltitude && it.flightLevelFt <= sector.maxAltitude }

            if (closestWindSegment != null) {
                windDelayAcc += calculateWindTimeAdjustmentInSegment(sector, closestWindSegment.wind)
            }
        }

        return windDelayAcc
    }

    private fun recalculateSequence(arrivals: List<TimelineOccurrence>) {
        val sequence = arrivals.sortedBy { it.time }.filterIsInstance<RunwayArrivalOccurrence>()
        var accumulatedDelay = 0.seconds

        for (i in 1 until sequence.size) {
            val previous = sequence[i - 1]
            val current = sequence[i]
            val next = sequence.getOrNull(i + 1)
            val timeBehind = current.time - previous.time
            val timeAheadOfNext = next?.time?.minus(current.time) ?: Duration.ZERO

            // Aircraft too close to the previous one -> needs to slow down
            if (timeBehind < minSeparation) {
                val timeToLose = minSeparation - timeBehind
                accumulatedDelay += timeToLose
                current.timeToLooseOrGain = accumulatedDelay
            }
            // Aircraft too close to the next one -> needs to speed up
            else if (timeAheadOfNext < minSeparation) {
                val timeToGain = minSeparation - timeAheadOfNext
                current.timeToLooseOrGain = -timeToGain

                // Reduce accumulated delay only if we gain time
                accumulatedDelay -= timeToGain
                if (accumulatedDelay < 0.seconds) accumulatedDelay = 0.seconds
            }
        }
    }

}

/**
 * Calculate the extra time caused by wind in a given segment.
 * @param sector The segment to calculate the wind delay for
 * @param windDirection The wind direction in degrees (true)
 * @param windSpeed The wind speed in knots
 * @return The extra time caused by wind in the segment (positive if delayed, negative if early)
 */
fun calculateWindTimeAdjustmentInSegment(
    sector: DescentProfileSegment,
    wind: Wind
): Duration {
    val windAngleRad = Math.toRadians((wind.directionDeg - sector.averageHeading).toDouble())
    val headWind = wind.speedKts * cos(windAngleRad)

    if (sector.duration.inWholeSeconds == 0L) {
        // Aircraft is just about to pass into the next segment
        return Duration.ZERO
    }

    val calculatedGroundSpeed = sector.distance / (sector.duration.inWholeSeconds / (60.0 * 60.0))

    val trueAirspeed = calculatedGroundSpeed + headWind

    if (trueAirspeed <= 0.0) {
        // Physically invalid, return zero delay
        return Duration.ZERO
    }

    val stillAirDuration = sector.duration * (calculatedGroundSpeed / trueAirspeed)
    return sector.duration - stillAirDuration
}
