package no.vaccsca.amandman.model.domain.valueobjects.atcClient

import no.vaccsca.amandman.model.domain.valueobjects.AircraftPosition
import no.vaccsca.amandman.model.domain.valueobjects.ArrivalState
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo
import no.vaccsca.amandman.model.domain.valueobjects.Waypoint

/**
 * All data about an arrival received from the ATC client.
 *
 * @param callsign The callsign of the aircraft.
 * @param icaoType The ICAO type of the aircraft.
 * @param assignedStar The name of the STAR assigned to the aircraft, if any.
 * @param assignedDirect The direct waypoint assigned to the aircraft, if any.
 * @param trackingController The position ID of the controller currently tracking.
 * @param scratchPad The scratchpad text for the aircraft, if any.
 * @param currentPosition The current position of the aircraft.
 * @param remainingWaypoints The remaining waypoints from the aircraft position, including the waypoints of the assigned STAR, if any. It should not include aircraft position, runway or airport.
 * @param assignedRunway The runway assigned to the aircraft, if any.
 * @param arrivalAirportIcao The ICAO code of the arrival airport.
 * @param flightPlanTas The true airspeed (TAS) from the flight plan,
 */
data class AtcClientArrivalData(
    val callsign: String,
    val icaoType: String,
    val assignedStar: String?,
    val assignedDirect: String?,
    val trackingController: String?,
    val scratchPad: String?,
    val currentPosition: AircraftPosition,
    val remainingWaypoints: List<Waypoint>,
    val assignedRunway: RunwayInfo?,
    val arrivalAirportIcao: String,
    val flightPlanTas: Int?,
)