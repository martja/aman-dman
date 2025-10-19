package no.vaccsca.amandman.model.domain.valueobjects.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.Flight
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus

data class RunwayArrivalEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    override val airportIcao: String,
    override val trackingController: String?,
    val assignedStar: String?,
    val assignedStarOk: Boolean,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val remainingDistance: Float,
    val withinActiveAdvisoryHorizon: Boolean,
    val sequenceStatus: SequenceStatus,
    val landingIas: Int,
    val distanceToPreceding: Float? = null,
) : RunwayEvent(timelineId, scheduledTime, airportIcao, runway, estimatedTime), Flight