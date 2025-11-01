package no.vaccsca.amandman.model.domain.valueobjects.timelineEvent

import kotlinx.datetime.Instant

sealed class RunwayFlightEvent(
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val airportIcao: String,
    open val callsign: String,
    open val icaoType: String,
    open val wakeCategory: Char,
    open val trackingController: String?,
) : RunwayEvent(scheduledTime, airportIcao, runway, estimatedTime)