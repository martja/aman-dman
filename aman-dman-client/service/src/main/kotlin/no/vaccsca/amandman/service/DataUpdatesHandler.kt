package no.vaccsca.amandman.service

import no.vaccsca.amandman.model.DataUpdateListener
import no.vaccsca.amandman.model.dto.RunwayStatus
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.weather.VerticalWeatherProfile

/**
 * Responsible for notifying the controller about data updates from the service.
 */
class GuiDataHandler : DataUpdateListener {
    lateinit var controller: DataUpdateListener

    override fun onLiveData(timelineEvents: List<TimelineEvent>) {
        controller.onLiveData(timelineEvents)
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        controller.onRunwayModesUpdated(airportIcao, runwayStatuses)
    }

    override fun onWeatherDataUpdated(data: VerticalWeatherProfile?) {
        controller.onWeatherDataUpdated(data)
    }

    override fun onMinimumSpacingUpdated(minimumSpacingNm: Double) {
        controller.onMinimumSpacingUpdated(minimumSpacingNm)
    }
}

/**
 * Responsible for sending data updates to HTTP server.
 */
class DataUpdatesServerSender : DataUpdateListener {
    override fun onLiveData(timelineEvents: List<TimelineEvent>) {
        println("Timeline events will be sent to server: ${timelineEvents.size} events")
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        println("Runway status update will be sent to server for $airportIcao: $runwayStatuses")
    }

    override fun onWeatherDataUpdated(data: VerticalWeatherProfile?) {
        println("Weather data update will be sent to server: $data")
    }

    override fun onMinimumSpacingUpdated(minimumSpacingNm: Double) {
        println("Minimum spacing update will be sent to server: $minimumSpacingNm")
    }
}
