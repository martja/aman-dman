package no.vaccsca.amandman.service

import kotlinx.datetime.Clock
import no.vaccsca.amandman.model.AircraftPerformance
import no.vaccsca.amandman.model.navigation.AircraftPosition
import no.vaccsca.amandman.model.navigation.RoutePoint
import no.vaccsca.amandman.model.navigation.star.Star
import no.vaccsca.amandman.integration.atcClient.entities.ArrivalJson
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.SequenceStatus
import no.vaccsca.amandman.model.navigation.LatLng
import no.vaccsca.amandman.model.weather.VerticalWeatherProfile
import no.vaccsca.amandman.service.DescentTrajectoryService.calculateDescentTrajectory

object EstimationService {
    fun ArrivalJson.toRunwayArrivalEvent(star: Star?, weatherData: VerticalWeatherProfile?, performance: AircraftPerformance): RunwayArrivalEvent? {
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

        val estimatedTime = Clock.System.now() + descentTrajectory.first().remainingTime

        return RunwayArrivalEvent(
            callsign = callsign,
            icaoType = icaoType,
            flightLevel = flightLevel,
            groundSpeed = groundSpeed,
            wakeCategory = performance.takeOffWTC,
            assignedStar = assignedStar,
            trackingController = trackingController,
            runway = assignedRunway!!,
            estimatedTime = estimatedTime,
            scheduledTime = estimatedTime,
            pressureAltitude = pressureAltitude,
            airportIcao = arrivalAirportIcao,
            descentTrajectory = descentTrajectory,
            timelineId = 0,
            assignedStarOk = star != null,
            withinActiveAdvisoryHorizon = false,
            sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE,
            landingIas = performance.landingVat
        )
    }
}