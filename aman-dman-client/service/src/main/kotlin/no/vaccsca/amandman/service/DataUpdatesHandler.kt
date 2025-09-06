package no.vaccsca.amandman.service

import no.vaccsca.amandman.integration.atcClient.entities.RunwayStatus
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.weather.VerticalWeatherProfile

interface DataUpdatesHandler {
    fun handleRunwayStatusUpdate(map: Map<String, Map<String, RunwayStatus>>)
    fun onWeatherDataUpdated(data: VerticalWeatherProfile?)
    fun onMinimumSpacingUpdated(nm: Double)
    fun onSequenceUpdated(airportIcao: String, sequencedArrivals: List<RunwayArrivalEvent>)
}

/**
 * Responsible for notifying the controller about data updates from the service.
 */
class DataUpdatesGuiUpdater : DataUpdatesHandler {
    lateinit var livedataInterface: LiveDataHandler

    override fun handleRunwayStatusUpdate(map: Map<String, Map<String, RunwayStatus>>) {
        map.forEach { (airportIcao, runwayStatuses) ->
            livedataInterface.onRunwayModesUpdated(airportIcao, runwayStatuses)
        }
    }

    override fun onWeatherDataUpdated(data: VerticalWeatherProfile?) {
        livedataInterface.onWeatherDataUpdated(data)
    }

    override fun onMinimumSpacingUpdated(nm: Double) {
        livedataInterface.onMinimumSpacingUpdated(nm)
    }

    override fun onSequenceUpdated(airportIcao: String, sequencedArrivals: List<RunwayArrivalEvent>) {
        livedataInterface.onLiveData(sequencedArrivals)
    }

}

/**
 * Responsible for sending data updates to HTTP server.
 */
class DataUpdatesServerSender : DataUpdatesHandler {
    override fun handleRunwayStatusUpdate(map: Map<String, Map<String, RunwayStatus>>) {
        println("Runway status update: $map")
    }

    override fun onWeatherDataUpdated(data: VerticalWeatherProfile?) {
        println("Weather data update: $data")
    }

    override fun onMinimumSpacingUpdated(nm: Double) {
        println("Minimum spacing update: $nm")
    }

    override fun onSequenceUpdated(airportIcao: String, sequencedArrivals: List<RunwayArrivalEvent>) {
        println("Sequence update for $airportIcao: $sequencedArrivals")
    }
}
