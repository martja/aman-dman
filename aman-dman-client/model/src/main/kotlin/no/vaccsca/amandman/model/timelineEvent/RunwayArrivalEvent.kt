package no.vaccsca.amandman.model.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.Flight
import no.vaccsca.amandman.model.SequenceStatus
import no.vaccsca.amandman.model.TrajectoryPoint

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
    val assignedStarOk: Boolean,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val trackingController: String?,
    val descentTrajectory: List<TrajectoryPoint>,
    val withinActiveAdvisoryHorizon: Boolean,
    val sequenceStatus: SequenceStatus,
    val landingIas: Int,
    val distanceToPreceding: Float? = null,
) : RunwayEvent(timelineId, scheduledTime, runway, airportIcao, estimatedTime), Flight