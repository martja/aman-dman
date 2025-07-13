package no.vaccsca.amandman.model.timelineEvent

import kotlinx.datetime.Instant

sealed class TimelineEvent(
    open val timelineId: Int,
    open val scheduledTime: Instant,
    open val airportIcao: String,
)