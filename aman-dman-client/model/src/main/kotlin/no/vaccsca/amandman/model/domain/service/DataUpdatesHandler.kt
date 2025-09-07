package no.vaccsca.amandman.model.domain.service

import no.vaccsca.amandman.model.domain.service.DataUpdateListener
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.data.dto.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile

/**
 * Responsible for notifying the presenter about data updates from the service.
 */
class GuiDataHandler : DataUpdateListener {
    lateinit var presenter: DataUpdateListener

    override fun onLiveData(timelineEvents: List<TimelineEvent>) {
        presenter.onLiveData(timelineEvents)
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        presenter.onRunwayModesUpdated(airportIcao, runwayStatuses)
    }

    override fun onWeatherDataUpdated(data: VerticalWeatherProfile?) {
        presenter.onWeatherDataUpdated(data)
    }

    override fun onMinimumSpacingUpdated(minimumSpacingNm: Double) {
        presenter.onMinimumSpacingUpdated(minimumSpacingNm)
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
