package no.vaccsca.amandman.model.domain.valueobjects.atcClient

import no.vaccsca.amandman.model.domain.valueobjects.ArrivalState
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo

data class AtcClientArrivalData(
    val callsign: String,
    val icaoType: String,
    val assignedRunway: RunwayInfo?,
    val assignedStar: String?,
    val assignedDirect: String?,
    val trackingController: String?,
    val scratchPad: String?,
    val currentState: ArrivalState,
    val arrivalAirportIcao: String,
    val flightPlanTas: Int?,
)