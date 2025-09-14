package no.vaccsca.amandman.model.domain.valueobjects.atcClient

import no.vaccsca.amandman.model.domain.valueobjects.AircraftPosition
import no.vaccsca.amandman.model.domain.valueobjects.RoutePoint

data class AtcClientArrivalData(
    val callsign: String,
    val icaoType: String,
    val position: AircraftPosition,
    val assignedRunway: String?,
    val assignedStar: String?,
    val assignedDirect: String?,
    val trackingController: String?,
    val scratchPad: String?,
    val track: Int,
    val route: List<RoutePoint>,
    val arrivalAirportIcao: String,
    val flightPlanTas: Int?,
)