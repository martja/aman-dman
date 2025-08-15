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

    private var sequence: Sequence = Sequence(
        //aah = emptyMap(),
        sequecencePlaces = emptyList(),
    )

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
            sequence = AmanDmanSequenceService.updateSequence(sequence, sequenceItems)
            latestArrivals = runwayArrivalEvents.map { arrivalEvent ->
                val sequenceSchedule = sequence.sequecencePlaces.find { it.item.id == arrivalEvent.callsign }?.scheduledTime
                arrivalEvent.copy(
                    scheduledTime = sequenceSchedule ?: arrivalEvent.scheduledTime,
                    sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
                )
            }

            livedataInterface.onLiveData(latestArrivals)
        }
    }

    private fun createRunwayArrivalEvents(arrivalJsons: List<ArrivalJson>) =
        arrivalJsons.mapNotNull { arrival ->
            arrival.toRunwayArrivalEvent(
                star = navdataRepository.stars.find { it.id == arrival.assignedStar && it.runway == arrival.assignedRunway },
                weatherData = weatherData,
                performance = AircraftPerformanceData.get(arrival.icaoType)
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

}