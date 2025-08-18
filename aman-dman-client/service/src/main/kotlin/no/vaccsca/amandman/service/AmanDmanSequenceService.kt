package no.vaccsca.amandman.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class Sequence(
    /**
     * A map that tracks whether an aircraft is currently within the Active Advisory Horizon (AAH).
     * Once an aircraft is in the AAH, it will be sequenced automatically and given a final scheduled time.
     */
    //val aah: Map<String, Boolean>,
    /**
     * A map that holds the current sequence of aircraft, where the key is the callsign
     * and the value is the scheduled time for that aircraft.
     */
    val sequecencePlaces: List<SequencePlace>
)

data class SequencePlace(
    val item: SequenceCandidate,
    val scheduledTime: Instant,
)

sealed class SequenceCandidate(
    open val id: String,
    open val preferredTime: Instant,
)

data class AircraftSequenceCandidate(
    val callsign: String,
    override val preferredTime: Instant,
    val landingIas: Int,
    val wakeCategory: Char
) : SequenceCandidate(
    id = callsign,
    preferredTime = preferredTime,
)

object AmanDmanSequenceService {

    val AAH_THRESHOLD: Duration = 30.minutes

    /**
     * Clears the current sequence, forcing a full rescheduling of all aircraft.
     * This is useful when the sequence needs to be recalculated from scratch.
     */
    fun reSchedule(currentSequence: Sequence): Sequence {
        return Sequence(
            sequecencePlaces = emptyList()
        )
    }

    /**
     * Suggests a new scheduled time for an aircraft in the sequence.
     * Used for manually adjusting the sequence when necessary.
     * Preceding aircraft in the sequence will be delayed if needed.
     */
    fun suggestScheduledTime(
        currentSequence: Sequence,
        callsign: String,
        scheduledTime: Instant,
        minimumSeparationNm: Double
    ): Sequence {
        val oldIdx = currentSequence.sequecencePlaces.indexOfFirst { it.item.id == callsign }
        if (oldIdx == -1) return currentSequence // Not found
        val oldPlace = currentSequence.sequecencePlaces[oldIdx]
        val updatedPlaces = currentSequence.sequecencePlaces.toMutableList()
        updatedPlaces.removeAt(oldIdx)

        // Find the new index where the aircraft should be inserted (by requested time)
        var insertIdx = updatedPlaces.indexOfFirst { it.scheduledTime > scheduledTime }
        if (insertIdx == -1) insertIdx = updatedPlaces.size

        // Determine the earliest valid time at or after requested time, considering spacing with new leader
        val prev = if (insertIdx > 0) updatedPlaces[insertIdx - 1] else null
        var newTime = scheduledTime
        if (prev != null) {
            val minTime = calculateSafeLandingTime(
                referenceTime = prev.scheduledTime,
                leader = prev.item as AircraftSequenceCandidate,
                follower = oldPlace.item as AircraftSequenceCandidate,
                minimumSeparationNm = minimumSeparationNm
            )
            if (newTime < minTime) newTime = minTime
        }
        // Insert the moved aircraft at the new index
        updatedPlaces.add(insertIdx, oldPlace.copy(scheduledTime = newTime))

        // Recalculate scheduled times for all following aircraft to maintain spacing and preferred time
        for (i in (insertIdx + 1) until updatedPlaces.size) {
            val leader = updatedPlaces[i - 1]
            val follower = updatedPlaces[i]
            val minTime = calculateSafeLandingTime(
                referenceTime = leader.scheduledTime,
                leader = leader.item as AircraftSequenceCandidate,
                follower = follower.item as AircraftSequenceCandidate,
                minimumSeparationNm = minimumSeparationNm
            )
            val nextTime = maxOf(minTime, follower.scheduledTime)
            updatedPlaces[i] = follower.copy(scheduledTime = nextTime)
        }
        return currentSequence.copy(sequecencePlaces = updatedPlaces)
    }

    /**
     * Removes an aircraft from the sequence and the Active Advisory Horizon (AAH),
     * allowing it to be re-sequenced.
     */
    fun removeFromSequence(sequence: Sequence, vararg callsigns: String): Sequence =
        sequence.copy(
            sequecencePlaces = sequence.sequecencePlaces.filter { it.item.id !in arrayOf(*callsigns) }.toList()
        )

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
        currentSequence: Sequence,
        callsign: String,
        requestedTime: Instant,
        minimumSeparationNm: Double = 3.0
    ): Boolean {
        val arrivalToCheck = findSequenceItem(currentSequence.sequecencePlaces.map { it.item }, callsign)

        if (arrivalToCheck == null) {
            // The aircraft is not in the sequence yet
            return false
        }

        val closestLeader = currentSequence.sequecencePlaces
            .filter { it.scheduledTime <= requestedTime }
            .maxByOrNull { it.scheduledTime }

        if (closestLeader == null) {
            // No preceding aircraft, so the time slot is available
            return true
        }

        // TODO: handle other than aircraft
        val safeLandingTime = calculateSafeLandingTime(
            referenceTime = closestLeader.scheduledTime,
            leader = closestLeader.item as AircraftSequenceCandidate,
            follower = arrivalToCheck as AircraftSequenceCandidate,
            minimumSeparationNm = minimumSeparationNm
        )

        return requestedTime >= safeLandingTime
    }

    fun updateSequence(
        currentSequence: Sequence,
        candidates: List<SequenceCandidate>,
        minimumSeparationNm: Double
    ): Sequence {
        val existingIds = currentSequence.sequecencePlaces.map { it.item.id }.toSet()
        val newCandidates = candidates.filter { it.id !in existingIds }.sortedBy { it.preferredTime }
        val newSequencePlaces = currentSequence.sequecencePlaces.sortedBy { it.scheduledTime }.toMutableList()

        for (newCandidate in newCandidates) {
            if (!newCandidate.isInAAH()) continue
            var scheduledTime = newCandidate.preferredTime

            while (true) {
                val leader = newSequencePlaces
                    .filter { it.scheduledTime <= scheduledTime }
                    .maxByOrNull { it.scheduledTime }

                if (leader != null) {
                    val requiredSpacingTime = calculateSafeLandingTime(
                        referenceTime = leader.scheduledTime,
                        leader = leader.item as AircraftSequenceCandidate,
                        follower = newCandidate as AircraftSequenceCandidate,
                        minimumSeparationNm = minimumSeparationNm
                    )
                    if (scheduledTime < requiredSpacingTime) {
                        scheduledTime = requiredSpacingTime
                        continue
                    }
                }

                val follower = newSequencePlaces
                    .filter { it.scheduledTime > scheduledTime }
                    .minByOrNull { it.scheduledTime }

                if (follower != null) {
                    val requiredFollowerTime = calculateSafeLandingTime(
                        referenceTime = scheduledTime,
                        leader = newCandidate as AircraftSequenceCandidate,
                        follower = follower.item as AircraftSequenceCandidate,
                        minimumSeparationNm = minimumSeparationNm
                    )
                    if (follower.scheduledTime < requiredFollowerTime) {
                        val effectiveSpacing = maxOf(
                            nmSpacingMap[Pair(newCandidate.wakeCategory, follower.item.wakeCategory)] ?: 3.0,
                            minimumSeparationNm
                        )
                        scheduledTime = follower.scheduledTime - nmToDuration(
                            effectiveSpacing,
                            follower.item.landingIas
                        )
                        continue
                    }
                }
                break
            }

            val insertIdx = newSequencePlaces.indexOfFirst { it.scheduledTime > scheduledTime }
            val place = SequencePlace(
                item = newCandidate as AircraftSequenceCandidate,
                scheduledTime = scheduledTime
            )
            if (insertIdx == -1) {
                newSequencePlaces.add(place)
            } else {
                newSequencePlaces.add(insertIdx, place)
            }
        }
        return Sequence(sequecencePlaces = newSequencePlaces)
    }

    private fun findSequenceItem(arrivals: List<SequenceCandidate>, callsign: String): SequenceCandidate? {
        return arrivals.find { it.id == callsign }
    }

    /**
     * Calculates the final scheduled time for an aircraft based on the leader's wake turbulence category.
     *
     * @param leaderSta The scheduled time of the preceding aircraft in the sequence.
     * @param leaderWtc The wake turbulence category of the preceding aircraft.
     * @param follower The aircraft for which the final time is being calculated.
     */
    private fun calculateSafeLandingTime(
        referenceTime: Instant,
        leader: AircraftSequenceCandidate,
        follower: AircraftSequenceCandidate,
        minimumSeparationNm: Double
    ): Instant {
        val wakeSpacingNm = nmSpacingMap[Pair(leader.wakeCategory, follower.wakeCategory)] ?: 3.0
        val effectiveSpacingNm = maxOf(wakeSpacingNm, minimumSeparationNm)
        val requiredSpacing = nmToDuration(effectiveSpacingNm, follower.landingIas)
        return referenceTime + requiredSpacing
    }

    /**
     * Checks if the aircraft is within the Active Advisory Horizon (AAH).
     */
    private fun SequenceCandidate.isInAAH(): Boolean {
        val remainingTime = this.preferredTime - Clock.System.now()
        return remainingTime < AAH_THRESHOLD
    }

    private val nmSpacingMap = mapOf(
        // Leader <> Follower
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
