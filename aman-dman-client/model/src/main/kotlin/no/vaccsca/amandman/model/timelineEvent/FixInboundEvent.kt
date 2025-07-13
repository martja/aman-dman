package no.vaccsca.amandman.model.timelineEvent

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.Flight
import no.vaccsca.amandman.model.TrajectoryPoint

data class FixInboundEvent(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    override val airportIcao: String,
    val assignedStar: String,
    val finalFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val trackingController: String,
    val finalFixEta: Instant,
    val descentProfile: List<TrajectoryPoint>,
) : RunwayEvent(timelineId, scheduledTime, runway, airportIcao, estimatedTime), Flight