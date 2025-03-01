package org.example.controller

import org.example.model.TimelineState
import org.example.state.Arrival
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimelineController(
    private val TimelineState: TimelineState
) {
    val minSeparationNauticalMiles = 3f
    val expectedSpeedOnFinalFix = 180f
    val minSeparation = (minSeparationNauticalMiles / expectedSpeedOnFinalFix * 60).roundToInt().minutes

    init {
        TimelineState.addListener { event ->
            if (event.propertyName == "timeNow") {
                recalculateSequence(TimelineState.arrivals)
            }
        }
    }

    private fun recalculateSequence(arrivals: List<Arrival>) {
        val sequence = arrivals.sortedBy { it.finalFixEta }
        var accumulatedDelay = 0.seconds

        for (i in 1 until sequence.size) {
            val previous = sequence[i - 1]
            val current = sequence[i]
            val next = sequence.getOrNull(i + 1)
            val timeBehind = current.finalFixEta - previous.finalFixEta
            val timeAheadOfNext = next?.finalFixEta?.minus(current.finalFixEta) ?: Duration.ZERO

            // Aircraft too close to the previous one -> needs to slow down
            if (timeBehind < minSeparation) {
                val timeToLose = minSeparation - timeBehind
                accumulatedDelay += timeToLose
                TimelineState.sequence[current.id] = accumulatedDelay
            }
            // Aircraft too close to the next one -> needs to speed up
            else if (timeAheadOfNext < minSeparation) {
                val timeToGain = minSeparation - timeAheadOfNext
                TimelineState.sequence[current.id] = -timeToGain

                // Reduce accumulated delay only if we gain time
                accumulatedDelay -= timeToGain
                if (accumulatedDelay < 0.seconds) accumulatedDelay = 0.seconds
            }
        }
    }

}