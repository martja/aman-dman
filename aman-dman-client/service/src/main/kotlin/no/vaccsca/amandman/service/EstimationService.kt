package no.vaccsca.amandman.service

import kotlinx.datetime.Clock
import no.vaccsca.amandman.common.AircraftPerformance
import no.vaccsca.amandman.common.dto.navigation.AircraftPosition
import no.vaccsca.amandman.common.dto.navigation.RoutePoint
import no.vaccsca.amandman.common.dto.navigation.star.Star
import integration.entities.ArrivalJson
import no.vaccsca.amandman.common.*
import no.vaccsca.amandman.common.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.common.SequenceStatus
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
            basedOnNavdata = star != null,
            withinActiveAdvisoryHorizon = false,
            sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE
        )
    }
}