package no.vaccsca.amandman.model

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AmanDmanSequence {

    /**
     * A map that holds the current sequence of aircraft, where the key is the callsign
     * and the value is the scheduled time for that aircraft.
     */
    private val currentSequence = mutableMapOf<String, Instant>() // callsign -> scheduled time

    /**
     * A reference to the latest arrivals that have been processed.
     */
    private var latestArrivals: List<RunwayArrivalEvent> = emptyList()

    /**
     * A map that tracks whether an aircraft is currently within the Active Advisory Horizon (AAH).
     * Once an aircraft is in the AAH, it will be sequenced automatically and given a final scheduled time.
     */
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

    /**
     * Check if an aircraft with the given wake turbulence category can be placed
     * in the sequence at the specified scheduled time. It can be placed if the separation
     * to the preceding aircraft is sufficient based on the wake turbulence category.
     * Succeeding aircraft are not considered in this check.
     *
     * TODO: also consider departures and runway closures.
     *
     * @param callsign The callsign of the aircraft being checked.
     * @param requestedTime The time slot being requested for the aircraft.
     */
    fun isTimeSlotAvailable(
        callsign: String,
        requestedTime: Instant
    ): Boolean {
        val arrivalToCheck = findArrivalEvent(callsign)

        if (arrivalToCheck == null) {
            // Something is wrong, the arrival event should exist
            println("Error: Arrival event for callsign $callsign not found in the latest arrivals.")
            return false
        }

        val closestLeader = currentSequence.entries
            .filter { it.value <= requestedTime }
            .maxByOrNull { it.value }

        if (closestLeader == null) {
            // No preceding aircraft, so the time slot is available
            return true
        }

        val (leaderCallsign, leaderSta) = closestLeader

        val leaderArrivalEvent = findArrivalEvent(leaderCallsign)

        if (leaderArrivalEvent == null) {
            // The leader event is not found, so we assume the time slot is available
            return true
        }

        val safeLandingTime = calculateSafeLandingTime(
            leaderSta = leaderSta,
            leaderWtc = leaderArrivalEvent.wakeCategory,
            follower = arrivalToCheck
        )

        return requestedTime >= safeLandingTime
    }

    fun updateSequence(arrivals: List<RunwayArrivalEvent>): List<TimelineEvent> {
        latestArrivals = arrivals

        val sorted = arrivals.sortedWith(compareBy(
            { currentSequence[it.callsign] ?: Instant.DISTANT_FUTURE },
            { it.estimatedTime }
        ))

        val result = mutableListOf<TimelineEvent>()
        var lastSequenced: RunwayArrivalEvent? = null
        var referenceTime = sorted.firstOrNull()?.estimatedTime ?: return emptyList()

        for (nextArrival in sorted) {
            val callsign = nextArrival.callsign
            val isNowInAah = nextArrival.isInAAH()
            val hasBeenSequenced = currentSequence.containsKey(callsign)

            if (hasBeenSequenced) {
                val updatedEvent = nextArrival.copy(
                    sequenceStatus = SequenceStatus.OK,
                    scheduledTime = currentSequence[callsign]!!
                )
                result += updatedEvent
                referenceTime = updatedEvent.scheduledTime
                lastSequenced = updatedEvent
                continue
            }

            if (isNowInAah) {
                inAah[callsign] = true
                val finalTime = if (lastSequenced == null) {
                    maxOf(nextArrival.estimatedTime, referenceTime)
                } else {
                    val earliestSafeLandingTime = calculateSafeLandingTime(referenceTime, lastSequenced.wakeCategory, nextArrival)
                    maxOf(nextArrival.estimatedTime, earliestSafeLandingTime)
                }
                currentSequence[callsign] = finalTime
                val updatedEvent = nextArrival.copy(
                    sequenceStatus = SequenceStatus.OK,
                    scheduledTime = finalTime
                )
                result += updatedEvent
                lastSequenced = updatedEvent
                referenceTime = finalTime
                continue
            }

            inAah[callsign] = false
            result += nextArrival.copy(sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE)
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
                findArrivalEvent(callsign)?.let { arrivalEvent ->
                    Triple(callsign, scheduledTime, arrivalEvent)
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
                val (leaderCallSign, _, _) = sorted[i - 1]
                val leader = findArrivalEvent(leaderCallSign) ?: continue
                val requiredSpacingTime = calculateSafeLandingTime(currentSequence[leaderCallSign]!!, leader.wakeCategory, occurrence)
                currentSequence[callsign] = maxOf(currentTime, occurrence.estimatedTime, requiredSpacingTime)
            }
        }
    }

    private fun findArrivalEvent(callsign: String): RunwayArrivalEvent? {
        return latestArrivals.find { it.callsign == callsign }
    }

    /**
     * Calculates the final scheduled time for an aircraft based on the leader's wake turbulence category.
     *
     * @param leaderSta The scheduled time of the preceding aircraft in the sequence.
     * @param leaderWtc The wake turbulence category of the preceding aircraft.
     * @param follower The aircraft for which the final time is being calculated.
     */
    private fun calculateSafeLandingTime(
        leaderSta: Instant,
        leaderWtc: Char,
        follower: RunwayArrivalEvent,
    ): Instant {
        val spacingNm = nmSpacingMap[Pair(leaderWtc, follower.wakeCategory)] ?: 3.0
        val requiredSpacing = nmToDuration(spacingNm, follower.landingIas)
        val earliestTime = leaderSta + requiredSpacing
        return earliestTime
    }

    /**
     * Checks if the aircraft is within the Active Advisory Horizon (AAH).
     */
    private fun RunwayArrivalEvent.isInAAH(): Boolean {
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

    private fun nmToDuration(distanceNm: Double, groundSpeedKt: Int): Duration {
        val hours = distanceNm / groundSpeedKt
        return (hours * 3600).seconds
    }

    /**
     * Automatically schedules aircraft inside the Active Advisory Horizon (AAH).
     */
    fun autoSchedueInsideAAH() {

    }
}
