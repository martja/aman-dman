package no.vaccsca.amandman.service

import no.vaccsca.amandman.integration.amanConfig.AircraftPerformanceData
import no.vaccsca.amandman.integration.atcClient.AtcClient
import kotlinx.datetime.Instant
import no.vaccsca.amandman.integration.atcClient.entities.ArrivalJson
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.weather.VerticalWeatherProfile
import no.vaccsca.amandman.integration.NavdataRepository
import no.vaccsca.amandman.model.SequenceStatus
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.service.EstimationService.toRunwayArrivalEvent

class AmanDataService(
    private val navdataRepository: NavdataRepository,
    private val atcClient: AtcClient
) {
    lateinit var livedataInterface: LiveDataHandler

    private var weatherData: VerticalWeatherProfile? = null

    // ID of the sequence, typically the airport ICAO code
    private val arrivalsCache: MutableMap<String, List<RunwayArrivalEvent>> = mutableMapOf()
    private val sequences: MutableMap<String, Sequence> = mutableMapOf()
    private var minimumSpacingNm = 3.0 // Minimum spacing in nautical miles

    fun subscribeForInbounds(icao: String) {
        sequences.computeIfAbsent(icao) { Sequence(emptyList()) }
        arrivalsCache.computeIfAbsent(icao) { emptyList() }

        atcClient.collectArrivalsFor(icao) { arrivals ->
            val runwayArrivalEvents = createRunwayArrivalEvents(arrivals)
            val sequenceItems = runwayArrivalEvents.map {
                AircraftSequenceCandidate(
                    callsign = it.callsign,
                    preferredTime = it.estimatedTime,
                    landingIas = it.landingIas,
                    wakeCategory = it.wakeCategory
                )
            }

            val aircraftToRemove = sequences[icao]!!.sequecencePlaces.map { it.item.id }.filter { it !in runwayArrivalEvents.map { it.callsign } }
            val cleanedSequence = AmanDmanSequenceService.removeFromSequence(sequences[icao]!!, *aircraftToRemove.toTypedArray())
            sequences[icao] = AmanDmanSequenceService.updateSequence(cleanedSequence, sequenceItems, minimumSpacingNm)
            arrivalsCache[icao] = runwayArrivalEvents.map { arrivalEvent ->
                val sequenceSchedule = sequences[icao]!!.sequecencePlaces.find { it.item.id == arrivalEvent.callsign }?.scheduledTime
                arrivalEvent.copy(
                    scheduledTime = sequenceSchedule ?: arrivalEvent.scheduledTime,
                    sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
                )
            }
            onSequenceUpdated(icao)
        }
    }

    private fun createRunwayArrivalEvents(arrivalJsons: List<ArrivalJson>) =
        arrivalJsons.mapNotNull { arrival ->
            val aircraftPerformance = try {
                AircraftPerformanceData.get(arrival.icaoType)
            } catch (e: Exception) {
                println("Error fetching performance data for ${arrival.icaoType}: ${e.message}")
                return@mapNotNull null
            }
            arrival.toRunwayArrivalEvent(
                star = navdataRepository.stars.find { it.id == arrival.assignedStar && it.runway == arrival.assignedRunway },
                weatherData = weatherData,
                performance = aircraftPerformance
            )
        }

    fun setMinimumSpacing(newSpacing: Double) {
        this.minimumSpacingNm = newSpacing
        sequences.forEach { (sequenceId, sequence) ->
            sequences[sequenceId] = AmanDmanSequenceService.reSchedule(sequence)
            onSequenceUpdated(sequenceId)
        }
    }

    fun updateWeatherData(data: VerticalWeatherProfile?) {
        weatherData = data
    }

    fun suggestScheduledTime(sequenceId: String, callsign: String, scheduledTime: Instant) {
        if (isTimeSlotAvailable(sequenceId, callsign, scheduledTime)) {
            sequences[sequenceId] = AmanDmanSequenceService.suggestScheduledTime(sequences[sequenceId]!!, callsign, scheduledTime, minimumSpacingNm)
            onSequenceUpdated(sequenceId)
        } else {
            println("Time slot is not available for $callsign at $scheduledTime")
        }
    }

    fun reSchedule(sequenceId: String, callSign: String?) {
        if (callSign == null) {
            sequences[sequenceId] = AmanDmanSequenceService.reSchedule(sequences[sequenceId]!!)
        } else {
            sequences[sequenceId] = AmanDmanSequenceService.removeFromSequence(sequences[sequenceId]!!, callSign)
        }
        onSequenceUpdated(sequenceId)
    }

    fun isTimeSlotAvailable(
        sequenceId: String,
        callsign: String,
        scheduledTime: Instant
    ): Boolean {
        return AmanDmanSequenceService.isTimeSlotAvailable(sequences[sequenceId]!!, callsign, scheduledTime)
    }

    /**
     * Automatically schedules aircraft inside the AAH (Active Advisory Horizon)
     */
    fun autoScheduleTimeline(timelineConfig: TimelineConfig) {
        AmanDmanSequenceService.autoSchedueInsideAAH()
    }

    /**
     * Refreshes the UI with the current sequence data by updating the latest arrivals
     * with the current sequence information and notifying the UI through the live data interface
     */
    private fun onSequenceUpdated(sequenceId: String) {
        val sequence = sequences[sequenceId] ?: return
        val latestArrivals = arrivalsCache[sequenceId] ?: return

        val updatedArrivals = latestArrivals
            .map { arrivalEvent -> arrivalEvent.updateScheduledTime(sequence) }
            .sortedByDescending { it.scheduledTime }

        val sequencedArrivals = if (updatedArrivals.size <= 1) {
            // Handle single or empty list - no zipWithNext needed
            updatedArrivals.map { it.withDistanceToPreceding(null) }
        } else {
            // Process pairs and add the last element
            val pairedArrivals = updatedArrivals.zipWithNext { a, b -> a.withDistanceToPreceding(b) }
            val lastArrival = updatedArrivals.last().withDistanceToPreceding(null)
            (pairedArrivals + lastArrival).reversed()
        }

        arrivalsCache[sequenceId] = sequencedArrivals
        livedataInterface.onLiveData(sequencedArrivals)
    }

    /**
     * Updates the scheduled time of the RunwayArrivalEvent based on the sequence information.
     * If the sequence has a scheduled time for the callsign, it updates the event's scheduled time
     * and sets the sequence status accordingly.
     */
    private fun RunwayArrivalEvent.updateScheduledTime(sequence: Sequence): RunwayArrivalEvent {
        val sequenceSchedule = sequence.sequecencePlaces.find { it.item.id == this.callsign }?.scheduledTime
        return this.copy(
            scheduledTime = sequenceSchedule ?: this.scheduledTime,
            sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
        )
    }

    /**
     * Calculates the distance to the preceding aircraft based on the
     * descent trajectory which included the remaining track miles
     */
    private fun RunwayArrivalEvent.withDistanceToPreceding(next: RunwayArrivalEvent?): RunwayArrivalEvent {
        val distanceToPreceding = if (next != null) {
            this.descentTrajectory.first().remainingDistance - next.descentTrajectory.first().remainingDistance
        } else {
            this.descentTrajectory.first().remainingDistance
        }
        return this.copy(distanceToPreceding = distanceToPreceding)
    }
}