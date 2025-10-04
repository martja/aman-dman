package no.vaccsca.amandman.model.domain.valueobjects.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.Flight
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo

data class DepartureEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    override val airportIcao: String,
    val sid: String
) : RunwayEvent(timelineId, scheduledTime, airportIcao, runway, estimatedTime), Flight