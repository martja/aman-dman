package no.vaccsca.amandman.model.domain.valueobjects.atcClient

import no.vaccsca.amandman.model.domain.valueobjects.AircraftPosition
import no.vaccsca.amandman.model.domain.valueobjects.ArrivalState
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo
import no.vaccsca.amandman.model.domain.valueobjects.Waypoint

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