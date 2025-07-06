package no.vaccsca.amandman.model

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.RunwayArrivalOccurrence
import no.vaccsca.amandman.common.SequenceStatus
import no.vaccsca.amandman.common.TimelineOccurrence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AmanDmanSequence {

    private val currentSequence = mutableMapOf<String, Instant>() // callsign -> scheduled time
    private var latestArrivals: List<RunwayArrivalOccurrence> = emptyList()

    val inAah = mutableMapOf<String, Boolean>()

    /**
     * Clears the current sequence, forcing a full rescheduling of all aircraft.
     * This is useful when the sequence needs to be recalculated from scratch.
     */
    fun reSchedule() {
        currentSequence.clear()
    }

    /**
     * Suggests a new scheduled time for an aircraft in the sequence.
     * Used for manually adjusting the sequence when necessary.
     * Preceding aircraft in the sequence will be moved accordingly.
     */
    fun suggestScheduledTime(callsign: String, scheduledTime: Instant) {
        currentSequence[callsign] = scheduledTime
        rebuildSequence()
    }

    /**
     * Removes an aircraft from the sequence and the Active Advisory Horizon (AAH),
     * allowing it to be re-sequenced.
     */
    fun removeFromSequence(callsign: String) {
        currentSequence.remove(callsign)
        inAah.remove(callsign)
    }

    fun updateSequence(arrivals: List<RunwayArrivalOccurrence>): List<TimelineOccurrence> {
        latestArrivals = arrivals

        val sorted = arrivals.sortedWith(compareBy(
            { currentSequence[it.callsign] ?: Instant.DISTANT_FUTURE },
            { it.estimatedTime }
        ))

        val result = mutableListOf<TimelineOccurrence>()
        var lastSequenced: RunwayArrivalOccurrence? = null
        var referenceTime = sorted.firstOrNull()?.estimatedTime ?: return emptyList()

        for (current in sorted) {
            val callsign = current.callsign
            val isNowInAah = current.isInAAH()
            val hasBeenSequenced = currentSequence.containsKey(callsign)

            if (hasBeenSequenced) {
                val updatedOccurrence = current.copy(
                    sequenceStatus = SequenceStatus.OK,
                    scheduledTime = currentSequence[callsign]!!
                )
                result += updatedOccurrence
                referenceTime = updatedOccurrence.scheduledTime
                lastSequenced = updatedOccurrence
                continue
            }

            if (isNowInAah) {
                inAah[callsign] = true
                val finalTime = if (lastSequenced == null) {
                    maxOf(current.estimatedTime, referenceTime)
                } else {
                    current.calculateFinalTime(referenceTime, lastSequenced)
                }
                currentSequence[callsign] = finalTime
                val updatedOccurrence = current.copy(
                    sequenceStatus = SequenceStatus.OK,
                    scheduledTime = finalTime
                )
                result += updatedOccurrence
                lastSequenced = updatedOccurrence
                referenceTime = finalTime
                continue
            }

            inAah[callsign] = false
            result += current.copy(sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE)
        }

        rebuildSequence()

        return result
    }

    /**
     * Recalculates scheduled times for all sequenced aircraft,
     * enforcing spacing based on currentSequence order.
     */
    private fun rebuildSequence() {
        val sorted = currentSequence.entries
            .mapNotNull { (callsign, scheduledTime) ->
                latestOccurrenceForCallsign(callsign)?.let { occurrence ->
                    Triple(callsign, scheduledTime, occurrence)
                }
            }
            .sortedBy { it.second }
            .toMutableList()

        for (i in sorted.indices) {
            val (callsign, _, occurrence) = sorted[i]
            val currentTime = currentSequence[callsign]!!

            if (i == 0) {
                currentSequence[callsign] = maxOf(currentTime, occurrence.estimatedTime)
            } else {
                val (leaderCs, _, _) = sorted[i - 1]
                val leaderLatest = latestOccurrenceForCallsign(leaderCs) ?: continue
                val requiredSpacingTime = occurrence.calculateFinalTime(currentSequence[leaderCs]!!, leaderLatest)
                currentSequence[callsign] = maxOf(currentTime, requiredSpacingTime)
            }
        }
    }

    private fun latestOccurrenceForCallsign(callsign: String): RunwayArrivalOccurrence? {
        return latestArrivals.find { it.callsign == callsign }
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

    /**
     * Checks if the aircraft is within the Active Advisory Horizon (AAH).
     */
    private fun RunwayArrivalOccurrence.isInAAH(): Boolean {
        return this.descentTrajectory.firstOrNull()!!.remainingTime < 30.minutes
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

    /**
     * Automatically schedules aircraft inside the Active Advisory Horizon (AAH).
     */
    fun autoSchedueInsideAAH() {

    }
}
