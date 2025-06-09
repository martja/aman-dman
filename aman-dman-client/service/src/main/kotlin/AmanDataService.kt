package org.example

import atcClient.AtcClient
import atcClient.AtcClientEuroScope
import org.example.EstimationService.toRunwayArrivalOccurrence
import org.example.config.AircraftPerformanceData
import org.example.eventHandling.LivedataInferface

class AmanDataService {
    var livedataInferface: LivedataInferface? = null
    private var weatherData: VerticalWeatherProfile? = null
    private var atcClient: AtcClient? = null
    private val navdataService: NavdataService = NavdataService()

    fun connectToAtcClient() {
        atcClient = AtcClientEuroScope("127.0.0.1", 12345)
    }

    fun subscribeForInbounds(icao: String) {
        atcClient?.collectArrivalsFor(icao) { arrivals ->
            livedataInferface?.onLiveData(arrivals.mapNotNull { arrival ->
                arrival.toRunwayArrivalOccurrence(
                    star = navdataService.stars.find { it.id == arrival.assignedStar && it.runway == arrival.assignedRunway },
                    weatherData = weatherData,
                    performance = AircraftPerformanceData.get(arrival.icaoType)
                )
            })
        }
    }

    fun updateWeatherData(data: VerticalWeatherProfile?) {
        weatherData = data
    }

}