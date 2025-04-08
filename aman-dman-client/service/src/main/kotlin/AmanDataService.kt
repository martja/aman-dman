package org.example

import atcClient.AtcClient
import atcClient.AtcClientEuroScope
import kotlinx.datetime.Clock
import org.example.DescentProfileService.generateDescentSegments
import org.example.config.AircraftPerformanceData
import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.lunip4l
import org.example.eventHandling.AmanDataListener
import org.example.integration.entities.ArrivalJson
import org.example.weather.WindApi

class AmanDataService {
    var amanDataListener: AmanDataListener? = null
    private var weatherData: VerticalWeatherProfile? = null
    private var atcClient: AtcClient? = null

    fun connectToAtcClient() {
        atcClient = AtcClientEuroScope("127.0.0.1", 12345)
    }

    fun subscribeForInbounds(icao: String) {
        atcClient?.collectArrivalsFor(icao) { arrivals ->
            amanDataListener?.onNewAmanData(arrivals.map { it.toRunwayArrivalOccurrence() })
        }
    }

    fun updateWeatherData(data: VerticalWeatherProfile?) {
        weatherData = data
    }

    fun ArrivalJson.toRunwayArrivalOccurrence(): RunwayArrivalOccurrence {
        val performance = AircraftPerformanceData.get(icaoType)
        val descentSegments = this.remainingRoute
            .map { RoutePoint(it.name, LatLng(it.latitude, it.longitude)) }
            .generateDescentSegments(
                AircraftPosition(
                    position = LatLng(latitude, longitude),
                    altitudeFt = flightLevel,
                    groundspeedKts = groundSpeed,
                    trackDeg = track
                ),
                weatherData,
                lunip4l,
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
            arrivalAirportIcao = arrivalAirportIcao,
            descentProfile = descentSegments,
            timelineId = 0,
        )
    }

}