package org.example

import atcClient.AtcClient
import atcClient.AtcClientEuroScope
import org.example.EstimationService.toRunwayArrivalOccurrence
import org.example.config.AircraftPerformanceData
import org.example.eventHandling.LivedataInferface
import org.example.integration.entities.ArrivalJson

class AmanDataService {
    var livedataInferface: LivedataInferface? = null
    private var weatherData: VerticalWeatherProfile? = null
    private var atcClient: AtcClient? = null
    private val navdataService: NavdataService = NavdataService()
    private val sequencingService: SequencingService = SequencingService()

    fun connectToAtcClient() {
        atcClient = AtcClientEuroScope("127.0.0.1", 12345)
    }

    fun subscribeForInbounds(icao: String) {
        atcClient?.collectArrivalsFor(icao) { arrivals ->
            val runwayArrivalOccurrences = createRunwayArrivalOccurrences(arrivals)
            val sequencingService = sequencingService.sequenceArrivals(runwayArrivalOccurrences)
            livedataInferface?.onLiveData(sequencingService)
        }
    }

    private fun createRunwayArrivalOccurrences(arrivalJsons: List<ArrivalJson>) =
        arrivalJsons.mapNotNull { arrival ->
            arrival.toRunwayArrivalOccurrence(
                star = navdataService.stars.find { it.id == arrival.assignedStar && it.runway == arrival.assignedRunway },
                weatherData = weatherData,
                performance = AircraftPerformanceData.get(arrival.icaoType)
            )
        }

    fun updateWeatherData(data: VerticalWeatherProfile?) {
        weatherData = data
    }

}