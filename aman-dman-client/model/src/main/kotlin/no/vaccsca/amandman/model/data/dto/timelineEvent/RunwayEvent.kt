package no.vaccsca.amandman.model.data.dto.timelineEvent

import kotlinx.datetime.Instant

sealed class RunwayEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val airportIcao: String,
    open val runway: String,
    open val estimatedTime: Instant,
) : TimelineEvent(timelineId, scheduledTime, airportIcao)