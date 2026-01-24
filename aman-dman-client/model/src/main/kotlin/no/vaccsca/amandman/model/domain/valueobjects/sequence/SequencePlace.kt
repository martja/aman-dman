package no.vaccsca.amandman.model.domain.valueobjects.sequence

import kotlinx.datetime.Instant

data class SequencePlace(
    val item: SequenceCandidate,
    val scheduledTime: Instant,
    val isManuallyAssigned: Boolean = false
)