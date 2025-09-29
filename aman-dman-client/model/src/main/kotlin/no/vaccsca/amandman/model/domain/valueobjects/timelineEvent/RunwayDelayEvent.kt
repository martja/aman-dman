package no.vaccsca.amandman.model.domain.valueobjects.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo
import kotlin.time.Duration

data class RunwayDelayEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: RunwayInfo,
    override val airportIcao: String,
    val delay: Duration,
    val name: String,
) : RunwayEvent(timelineId, scheduledTime, airportIcao, runway, estimatedTime)