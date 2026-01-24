package no.vaccsca.amandman.model.domain.valueobjects.sequence

import kotlinx.datetime.Instant

sealed class SequenceCandidate(
    open val id: String,
    open val preferredTime: Instant,
)
