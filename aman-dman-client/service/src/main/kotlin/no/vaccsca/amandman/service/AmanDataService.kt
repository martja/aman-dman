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

    private var latestArrivals = listOf<RunwayArrivalEvent>()

    private var sequence: Sequence = Sequence(emptyList())
        set(value) = run {
            field = value
            onSequenceUpdated()
        }

    fun subscribeForInbounds(icao: String) {
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
            val aircraftToRemove = sequence.sequecencePlaces.map { it.item.id }.filter { it !in runwayArrivalEvents.map { it.callsign } }
            val cleanedSequence = AmanDmanSequenceService.removeFromSequence(sequence, *aircraftToRemove.toTypedArray())
            sequence = AmanDmanSequenceService.updateSequence(cleanedSequence, sequenceItems)
            latestArrivals = runwayArrivalEvents.map { arrivalEvent ->
                val sequenceSchedule = sequence.sequecencePlaces.find { it.item.id == arrivalEvent.callsign }?.scheduledTime
                arrivalEvent.copy(
                    scheduledTime = sequenceSchedule ?: arrivalEvent.scheduledTime,
                    sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
                )
            }
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

    fun updateWeatherData(data: VerticalWeatherProfile?) {
        weatherData = data
    }

    fun suggestScheduledTime(callsign: String, scheduledTime: Instant) {
        if (isTimeSlotAvailable(callsign, scheduledTime)) {
            sequence = AmanDmanSequenceService.suggestScheduledTime(sequence, callsign, scheduledTime)
        } else {
            println("Time slot is not available for $callsign at $scheduledTime")
        }
    }

    fun reSchedule(callSign: String?) {
        if (callSign == null) {
            sequence = AmanDmanSequenceService.reSchedule(sequence)
        } else {
            sequence = AmanDmanSequenceService.removeFromSequence(sequence, callSign)
        }
    }

    fun isTimeSlotAvailable(
        callsign: String,
        scheduledTime: Instant
    ): Boolean {
        return AmanDmanSequenceService.isTimeSlotAvailable(sequence, callsign, scheduledTime)
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
    private fun onSequenceUpdated() {
        val updatedArrivals = latestArrivals
            .map { arrivalEvent -> arrivalEvent.updateScheduledTime() }
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

        latestArrivals = sequencedArrivals
        livedataInterface.onLiveData(sequencedArrivals)
    }

    /**
     * Updates the scheduled time of the RunwayArrivalEvent based on the sequence information.
     * If the sequence has a scheduled time for the callsign, it updates the event's scheduled time
     * and sets the sequence status accordingly.
     */
    private fun RunwayArrivalEvent.updateScheduledTime(): RunwayArrivalEvent {
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