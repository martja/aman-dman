package no.vaccsca.amandman.model.domain.valueobjects.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo

sealed class RunwayEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val airportIcao: String,
    open val runway: RunwayInfo,
    open val estimatedTime: Instant,
) : TimelineEvent(timelineId, scheduledTime, airportIcao)