package org.example

import kotlinx.datetime.Clock
import org.example.DescentTrajectoryService.calculateDescentTrajectory
import org.example.config.AircraftPerformanceData
import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.Star
import org.example.integration.entities.ArrivalJson

object EstimationService {
    fun ArrivalJson.toRunwayArrivalOccurrence(star: Star?, weatherData: VerticalWeatherProfile?): RunwayArrivalOccurrence? {

        val performance =
            try { AircraftPerformanceData.get(icaoType) }
            catch (e: IllegalArgumentException) { return null }


        if (assignedRunway == null) {
            return null
        }

        val descentTrajectory = this.route
            .map { RoutePoint(it.name, LatLng(it.latitude, it.longitude), it.isPassed, it.isOnStar) }
            .calculateDescentTrajectory(
                AircraftPosition(
                    position = LatLng(latitude, longitude),
                    altitudeFt = flightLevel,
                    groundspeedKts = groundSpeed,
                    trackDeg = track
                ),
                weatherData,
                star,
                performance,
                arrivalAirportIcao,
                flightPlanTas,
            )

        return RunwayArrivalOccurrence(
            callsign = callsign,
            icaoType = icaoType,
            flightLevel = flightLevel,
            groundSpeed = groundSpeed,
            wakeCategory = performance.takeOffWTC,
            assignedStar = assignedStar,
            trackingController = trackingController,
            runway = assignedRunway!!,
            time = Clock.System.now() + descentTrajectory.first().remainingTime,
            pressureAltitude = pressureAltitude,
            airportIcao = arrivalAirportIcao,
            descentTrajectory = descentTrajectory,
            timelineId = 0,
            basedOnNavdata = star != null,
        )
    }
}