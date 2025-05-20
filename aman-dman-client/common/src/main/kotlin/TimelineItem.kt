package org.example

import kotlinx.datetime.Instant
import kotlin.time.Duration

sealed class TimelineOccurrence(
    open val timelineId: Int,
    open val time: Instant,
    open val airportIcao: String,
)

sealed class RunwayOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val airportIcao: String,
    open val runway: String,
) : TimelineOccurrence(timelineId, time, airportIcao)

interface Flight {
    val callsign: String
    val icaoType: String
    val wakeCategory: Char
}

data class FixInboundOccurrence(
    override val timelineId: Int,
    override val time: Instant,
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
    var timeToLooseOrGain: Duration? = null,
    val descentProfile: List<TrajectoryPoint>,
    var windDelay: Duration? = null
) : RunwayOccurrence(timelineId, time, runway, airportIcao), Flight

data class RunwayDelayOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val runway: String,
    override val airportIcao: String,
    val delay: Duration,
    val name: String,
) : RunwayOccurrence(timelineId, time, runway, airportIcao)

data class DepartureOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    override val airportIcao: String,
    val sid: String
) : RunwayOccurrence(timelineId, time, runway, airportIcao), Flight

data class RunwayArrivalOccurrence(
    override val timelineId: Int,
    override val time: Instant,
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
    var timeToLooseOrGain: Duration? = null
) : RunwayOccurrence(timelineId, time, runway, airportIcao), Flight

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
    val runwayLeft: String?,
    val runwayRight: String?,
    val targetFixesLeft: List<String>,
    val targetFixesRight: List<String>,
    val airportIcao: String,
)