package no.vaccsca.amandman.model.timelineEvent

import kotlinx.datetime.Instant
import kotlin.time.Duration

data class RunwayDelayEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val airportIcao: String,
    val delay: Duration,
    val name: String,
) : RunwayEvent(timelineId, scheduledTime, runway, airportIcao, estimatedTime)