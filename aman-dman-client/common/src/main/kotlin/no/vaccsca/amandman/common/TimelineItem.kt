package no.vaccsca.amandman.common

import kotlinx.datetime.Instant
import kotlin.time.Duration

sealed class TimelineOccurrence(
    open val timelineId: Int,
    open val scheduledTime: Instant,
    open val airportIcao: String,
)

sealed class RunwayOccurrence(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val airportIcao: String,
    open val runway: String,
    open val estimatedTime: Instant,
) : TimelineOccurrence(timelineId, scheduledTime, airportIcao)

interface Flight {
    val callsign: String
    val icaoType: String
    val wakeCategory: Char
}

data class FixInboundOccurrence(
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
) : RunwayOccurrence(timelineId, scheduledTime, runway, airportIcao, estimatedTime), Flight

data class RunwayDelayOccurrence(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val airportIcao: String,
    val delay: Duration,
    val name: String,
) : RunwayOccurrence(timelineId, scheduledTime, runway, airportIcao, estimatedTime)

data class DepartureOccurrence(
    override val timelineId: Int,
    override val scheduledTime: Instant,
    override val estimatedTime: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    override val airportIcao: String,
    val sid: String
) : RunwayOccurrence(timelineId, scheduledTime, runway, airportIcao, estimatedTime), Flight

data class RunwayArrivalOccurrence(
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
) : RunwayOccurrence(timelineId, scheduledTime, runway, airportIcao, estimatedTime), Flight

data class TrajectoryPoint(
    val fixId: String?,
    val position: LatLng,
    val altitude: Int,
    val remainingDistance: Float,
    val remainingTime: Duration,
    val groundSpeed: Int,
    val tas: Int,
    val ias: Int,
    val wind: Wind,
    val heading: Int,
)

data class DescentStep(
    val position: LatLng,
    val altitudeFt: Int,
    val groundSpeed: Int,
    val tas: Int,
    val ias: Int,
    val wind: Wind
)

data class TimelineConfig(
    val title: String,
    val runwaysLeft: List<String>,
    val runwaysRight: List<String>,
    val targetFixesLeft: List<String>,
    val targetFixesRight: List<String>,
    val airportIcao: String,
)

enum class SequenceStatus {
    AWAITING_FOR_SEQUENCE,
    OK,
    FOR_MANUAL_REINSERTION,
}