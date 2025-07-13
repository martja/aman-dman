package no.vaccsca.amandman.service

import config.AircraftPerformanceData
import no.vaccsca.amandman.integration.atcClient.AtcClient
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.eventHandling.LivedataInferface
import integration.entities.ArrivalJson
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.common.VerticalWeatherProfile
import no.vaccsca.amandman.model.AmanDmanSequence
import no.vaccsca.amandman.model.NavdataRepository
import no.vaccsca.amandman.service.EstimationService.toRunwayArrivalEvent

class AmanDataService(
    private val amanDmanSequence: AmanDmanSequence,
    private val navdataRepository: NavdataRepository,
    private val atcClient: AtcClient
) {
    lateinit var livedataInterface: LivedataInferface

    private var weatherData: VerticalWeatherProfile? = null

    fun subscribeForInbounds(icao: String) {
        atcClient.collectArrivalsFor(icao) { arrivals ->
            val runwayArrivalEvents = createRunwayArrivalEvents(arrivals)
            val sequencedArrivals = amanDmanSequence.updateSequence(runwayArrivalEvents)
            livedataInterface.onLiveData(sequencedArrivals)
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
        amanDmanSequence.suggestScheduledTime(callsign, scheduledTime)
    }

    fun reSchedule(callSign: String?) {
        if (callSign == null) {
            amanDmanSequence.reSchedule()
        } else {
            amanDmanSequence.removeFromSequence(callSign)
        }
    }

    /**
     * Automatically schedules aircraft inside the AAH (Active Advisory Horizon)
     */
    fun autoScheduleTimeline(timelineConfig: TimelineConfig) {
        amanDmanSequence.autoSchedueInsideAAH()
    }

}