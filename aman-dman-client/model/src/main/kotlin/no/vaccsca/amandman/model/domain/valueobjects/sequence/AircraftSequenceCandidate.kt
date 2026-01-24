package no.vaccsca.amandman.model.domain.valueobjects.sequence

import kotlinx.datetime.Instant

data class AircraftSequenceCandidate(
    val callsign: String,
    override val preferredTime: Instant,
    val landingIas: Int,
    val wakeCategory: Char,
    val assignedRunway: String?
) : SequenceCandidate(
    id = callsign,
    preferredTime = preferredTime,
)