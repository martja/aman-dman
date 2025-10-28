package no.vaccsca.amandman.model.domain.valueobjects.timelineEvent

import kotlinx.datetime.Instant

sealed class TimelineEvent(
    open val scheduledTime: Instant,
    open val airportIcao: String,
)