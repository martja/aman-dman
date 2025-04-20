package org.example

import atcClient.AtcClient
import atcClient.AtcClientEuroScope
import kotlinx.datetime.Clock
import org.example.DescentProfileService.generateDescentSegments
import org.example.config.AircraftPerformanceData
import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.eventHandling.AmanDataListener
import org.example.integration.entities.ArrivalJson

class AmanDataService {
    var amanDataListener: AmanDataListener? = null
    private var weatherData: VerticalWeatherProfile? = null
    private var atcClient: AtcClient? = null
    private val navdataService: NavdataService = NavdataService()

    fun connectToAtcClient() {
        atcClient = AtcClientEuroScope("127.0.0.1", 12345)
    }

    fun subscribeForInbounds(icao: String) {
        atcClient?.collectArrivalsFor(icao) { arrivals ->
            amanDataListener?.onLiveData(arrivals.map { it.toRunwayArrivalOccurrence() })
        }
    }

    fun updateWeatherData(data: VerticalWeatherProfile?) {
        weatherData = data
    }

    private fun ArrivalJson.toRunwayArrivalOccurrence(): RunwayArrivalOccurrence {
        val performance = AircraftPerformanceData.get(icaoType)
        val star = navdataService.stars.find { it.id == assignedStar && it.runway == assignedRunway }

        if (star == null) {
            println("Star not found for ${this.callsign}: $assignedStar")
        }

        val descentSegments = this.remainingRoute
            .map { RoutePoint(it.name, LatLng(it.latitude, it.longitude)) }
            .let { route ->
                listOf(RoutePoint("CURRENT", LatLng(latitude, longitude))) + route
            }
            .generateDescentSegments(
                AircraftPosition(
                    position = LatLng(latitude, longitude),
                    altitudeFt = flightLevel,
                    groundspeedKts = groundSpeed,
                    trackDeg = track
                ),
                weatherData,
                star,
                performance
            )

        return RunwayArrivalOccurrence(
            callsign = callsign,
            icaoType = icaoType,
            flightLevel = flightLevel,
            groundSpeed = groundSpeed,
            wakeCategory = performance.takeOffWTC,
            assignedStar = assignedStar,
            trackingController = trackingController,
            runway = assignedRunway,
            time = Clock.System.now() + descentSegments.first().remainingTime,
            pressureAltitude = pressureAltitude,
            airportIcao = arrivalAirportIcao,
            descentProfile = descentSegments,
            timelineId = 0,
            basedOnNavdata = star != null,
        )
    }

}