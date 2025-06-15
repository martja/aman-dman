package org.example

import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SequencingService {

    private val schedule = mutableMapOf<String, Instant>() // callsign -> scheduled time
    val inAah = mutableMapOf<String, Boolean>()

    fun sequenceArrivals(arrivals: List<RunwayArrivalOccurrence>): List<TimelineOccurrence> {
        val sorted = arrivals.sortedWith(compareBy(
            { schedule[it.callsign] ?: Instant.DISTANT_FUTURE },
            { it.estimatedTime }
        ))

        val result = mutableListOf<TimelineOccurrence>()

        var lastSequenced: RunwayArrivalOccurrence? = null
        var referenceTime = sorted.firstOrNull()?.estimatedTime ?: return emptyList()

        for (current in sorted) {
            val callsign = current.callsign
            val wasInAah = inAah[callsign]
            val isNowInAah = current.isInAAH()
            val hasBeenSequenced = schedule.containsKey(callsign)

            when {
                callsign in schedule -> {
                    // Aircraft has been sequenced before, use existing scheduled time
                    val scheduledTime = schedule[callsign]!!
                    result += current.copy(
                        sequenceStatus = SequenceStatus.OK,
                        scheduledTime = scheduledTime
                    )
                    referenceTime = scheduledTime
                    lastSequenced = current
                    continue
                }
                !hasBeenSequenced && wasInAah == false && isNowInAah -> {
                    inAah[callsign] = true
                    // Assign actual scheduled time (with spacing) when entering AAH
                    val finalTime = if (lastSequenced == null) {
                        maxOf(current.estimatedTime, referenceTime)
                    } else {
                        current.calculateFinalTime(referenceTime, lastSequenced)
                    }
                    schedule[callsign] = finalTime
                    result += current.copy(
                        sequenceStatus = SequenceStatus.OK,
                        scheduledTime = finalTime
                    )
                    lastSequenced = current
                    continue
                }

                !hasBeenSequenced && wasInAah == null && isNowInAah -> {
                    inAah[callsign] = true
                    result += current.copy(sequenceStatus = SequenceStatus.NEEDS_MANUAL_INSERTION)
                    continue
                }

                !hasBeenSequenced && !isNowInAah -> {
                    inAah[callsign] = false
                    result += current.copy(sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE)
                    continue
                }
                else ->
                    result += current.copy()

            }
        }

        return result
    }

    private fun RunwayArrivalOccurrence.calculateFinalTime(
        referenceTime: Instant,
        leader: RunwayArrivalOccurrence
    ): Instant {
        val spacingNm = nmSpacingMap[Pair(leader.wakeCategory, this.wakeCategory)] ?: 3.0
        val requiredSpacing = nmToDuration(spacingNm)
        val earliestTime = referenceTime + requiredSpacing
        return maxOf(this.estimatedTime, earliestTime)
    }

    private fun RunwayArrivalOccurrence.isInAAH(): Boolean {
        return this.descentTrajectory.firstOrNull()?.remainingDistance!! < 100
    }

    private val nmSpacingMap = mapOf(
        Pair('H', 'H') to 4.0,
        Pair('H', 'M') to 5.0,
        Pair('H', 'L') to 6.0,
        Pair('M', 'L') to 5.0,
        Pair('J', 'H') to 6.0,
        Pair('J', 'M') to 7.0,
        Pair('J', 'L') to 8.0,
    )

    private fun nmToDuration(distanceNm: Double, groundSpeedKt: Double = 140.0): Duration {
        val hours = distanceNm / groundSpeedKt
        return (hours * 3600).seconds
    }

    fun getScheduledTime(callsign: String): Instant? = schedule[callsign]
}
