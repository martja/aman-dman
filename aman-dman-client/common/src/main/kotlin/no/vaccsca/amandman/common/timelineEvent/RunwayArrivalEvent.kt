package no.vaccsca.amandman.common.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.Flight
import no.vaccsca.amandman.common.SequenceStatus
import no.vaccsca.amandman.common.TrajectoryPoint

data class RunwayArrivalEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    override val airportIcao: String,
    val assignedStar: String?,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val trackingController: String?,
    val descentTrajectory: List<TrajectoryPoint>,
    val basedOnNavdata: Boolean,
    val withinActiveAdvisoryHorizon: Boolean,
    val sequenceStatus: SequenceStatus
) : RunwayEvent(timelineId, scheduledTime, runway, airportIcao, estimatedTime), Flight