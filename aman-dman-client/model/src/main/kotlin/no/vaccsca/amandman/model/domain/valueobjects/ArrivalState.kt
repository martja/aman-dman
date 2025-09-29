package no.vaccsca.amandman.model.domain.valueobjects

/**
 * Represents the remaining route for an aircraft
 *
 * @param currentPosition The current position of the aircraft.
 * @param remainingWaypoints The list of waypoints that make up the arrival route, excluding the current position and arrival runway.
 * @param assignedRunway The runway information for the intended landing.
 */
data class ArrivalState(
    val currentPosition: AircraftPosition,
    val remainingWaypoints: List<Waypoint>,
    val assignedRunway: RunwayInfo,
)