package no.vaccsca.amandman.model.domain.valueobjects.atcClient

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.AircraftPosition
import no.vaccsca.amandman.model.domain.valueobjects.Waypoint

/**
 * All data about a departure received from the ATC client.
 */
data class AtcClientDepartureData(
    val departureIcao: String,
    val callsign: String,
    val icaoType: String,
    val assignedSid: String?,
    val trackingController: String?,
    val scratchPad: String?,
    val assignedRunway: String?,
    val wakeCategory: Char,
)