package no.vaccsca.amandman.model.domain.valueobjects

import no.vaccsca.amandman.model.domain.enums.NonSequencedReason

data class NonSequencedEvent(
    val callsign: String,
    val aircraftType: String,
    val wakeCategory: Char?,
    val reason: NonSequencedReason,
)