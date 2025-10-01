package no.vaccsca.amandman.model.domain.service

import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.data.integration.SharedStateHttpClient
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile

/**
 * Responsible for notifying the presenter about data updates from the service.
 */
class GuiDataHandler : DataUpdateListener {
    lateinit var presenter: DataUpdateListener

    override fun onLiveData(airportIcao: String, timelineEvents: List<TimelineEvent>) {
        presenter.onLiveData(airportIcao, timelineEvents)
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        presenter.onRunwayModesUpdated(airportIcao, runwayStatuses)
    }

    override fun onWeatherDataUpdated(airportIcao: String, data: VerticalWeatherProfile?) {
        presenter.onWeatherDataUpdated(airportIcao, data)
    }

    override fun onMinimumSpacingUpdated(airportIcao: String, minimumSpacingNm: Double) {
        presenter.onMinimumSpacingUpdated(airportIcao, minimumSpacingNm)
    }
}

/**
 * Responsible for sending data updates to HTTP server.
 */
class DataUpdatesServerSender : DataUpdateListener {
    val sharedStateHttpClient = SharedStateHttpClient()

    override fun onLiveData(airportIcao: String, timelineEvents: List<TimelineEvent>) {
        sharedStateHttpClient.sendTimelineEvents(airportIcao, timelineEvents)
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        sharedStateHttpClient.sendRunwayStatuses(airportIcao, runwayStatuses)
    }

    override fun onWeatherDataUpdated(airportIcao: String, data: VerticalWeatherProfile?) {
        sharedStateHttpClient.sendWeatherData(airportIcao, data)
    }

    override fun onMinimumSpacingUpdated(airportIcao: String, minimumSpacingNm: Double) {
        sharedStateHttpClient.sendMinimumSpacing(airportIcao, minimumSpacingNm)
    }
}
