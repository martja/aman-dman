package no.vaccsca.amandman.model.domain.service

import kotlinx.datetime.Clock
import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.model.data.repository.AircraftPerformanceData
import no.vaccsca.amandman.model.domain.exception.EmptyTrajectoryException
import no.vaccsca.amandman.model.domain.exception.NoAssignedRunwayException
import no.vaccsca.amandman.model.domain.exception.ReachedEndOfRouteException
import no.vaccsca.amandman.model.domain.exception.UnknownAircraftTypeException
import no.vaccsca.amandman.model.domain.valueobjects.Airport
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.time.Duration.Companion.seconds

/**
 * Maps data from the ATC client to domain objects used for planning.
 */
object ArrivalEventService {

    private val descentTrajectoryCache = mutableMapOf<String, List<TrajectoryPoint>>()

    fun createRunwayArrivalEvent(airport: Airport, arrival: AtcClientArrivalData, weatherData: VerticalWeatherProfile?): RunwayArrivalEvent {
        val aircraftPerformance = try {
            AircraftPerformanceData.get(arrival.icaoType)
        } catch (_: IllegalArgumentException) {
            throw UnknownAircraftTypeException("Unsupported aircraft type: ${arrival.icaoType}")
        }

        if (arrival.assignedRunway == null) {
            throw NoAssignedRunwayException("Arrival has no runway assigned")
        }

        val trajectory = DescentTrajectoryService.calculateDescentTrajectory(
            currentPosition = arrival.currentPosition,
            assignedRunway = arrival.assignedRunway,
            remainingWaypoints = arrival.remainingWaypoints,
            verticalWeatherProfile = weatherData,
            assignedStar = arrival.assignedStar,
            aircraftPerformance = aircraftPerformance,
            flightPlanTas = arrival.flightPlanTas,
            airport = airport
        )

        if (trajectory == null) {
            throw ReachedEndOfRouteException("Failed to calculate descent trajectory for ${arrival.callsign}")
        }

        descentTrajectoryCache[arrival.callsign] = trajectory.trajectoryPoints

        if (trajectory.trajectoryPoints.isEmpty()) {
            throw EmptyTrajectoryException("The descent trajectory is empty")
        }

        val estimatedTime = NtpClock.now() + (trajectory.trajectoryPoints.firstOrNull()?.remainingTime ?: 0.seconds)

        return RunwayArrivalEvent(
            callsign = arrival.callsign,
            icaoType = arrival.icaoType,
            flightLevel = arrival.currentPosition.flightLevel,
            groundSpeed = arrival.currentPosition.groundspeedKts,
            wakeCategory = aircraftPerformance.takeOffWTC,
            assignedStar = arrival.assignedStar,
            trackingController = arrival.trackingController,
            runway = arrival.assignedRunway,
            estimatedTime = estimatedTime,
            scheduledTime = estimatedTime,
            pressureAltitude = arrival.currentPosition.altitudeFt,
            airportIcao = arrival.arrivalAirportIcao,
            remainingDistance = trajectory.trajectoryPoints.first().remainingDistance,
            assignedStarOk = trajectory.star != null,
            withinActiveAdvisoryHorizon = false,
            sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE,
            landingIas = aircraftPerformance.landingVat,
            scratchPad = arrival.scratchPad,
            assignedDirect = arrival.assignedDirect,
            lastTimestamp = NtpClock.now()
        )
    }

    fun getDescentProfileForCallsign(callsign: String): List<TrajectoryPoint>? {
        return descentTrajectoryCache[callsign]
    }
}