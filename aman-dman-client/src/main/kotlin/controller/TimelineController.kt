package org.example.controller

import kotlinx.datetime.Instant
import model.entities.TimelineConfig
import org.example.integration.AtcClient
import org.example.model.*
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimelineController(
    private val timelineState: TimelineState,
    atcClient: AtcClient,
    timelineConfig: TimelineConfig
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

        atcClient.collectInboundsForFix(
            targetFixes = listOf(timelineConfig.targetFixLeft, timelineConfig.targetFixRight!!),
            viaFixes = timelineConfig.viaFixes,
            destinationAirports = timelineConfig.airports,
            onDataReceived = this::handleArrivals
        )

        atcClient.collectDeparturesFrom(
            airportIcao = timelineConfig.airports.first(),
            onDataReceived = this::handleDepartures
        )
    }

    private fun handleArrivals(fixInboundOccurrences: List<FixInboundOccurrence>) {
        timelineState.arrivalOccurrences = fixInboundOccurrences
    }

    private fun handleDepartures(departureOccurrences: List<DepartureOccurrence>) {
        timelineState.departureOccurrences = departureOccurrences
    }

    fun addDelayDefinition(name: String, from: Instant, duration: Duration, runway: String) {
        timelineState.addDelayDefinition(name, from, duration, runway)
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