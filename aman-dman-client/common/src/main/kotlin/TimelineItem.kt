package org.example

import kotlinx.datetime.Instant
import kotlin.time.Duration

sealed class TimelineOccurrence(
    open val timelineId: Int,
    open val time: Instant
)

sealed class RunwayOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    open val runway: String
) : TimelineOccurrence(timelineId, time)

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
    val assignedStar: String,
    val finalFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val trackingController: String,
    val finalFixEta: Instant,
    val arrivalAirportIcao: String,
    var timeToLooseOrGain: Duration? = null,
    val descentProfile: List<DescentSegment>,
    var windDelay: Duration? = null
) : RunwayOccurrence(timelineId, time, runway), Flight

data class RunwayDelayOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val runway: String,
    val delay: Duration,
    val name: String
) : RunwayOccurrence(timelineId, time, runway)

data class DepartureOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    val sid: String
) : RunwayOccurrence(timelineId, time, runway), Flight

data class RunwayArrivalOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    val assignedStar: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val arrivalAirportIcao: String,
    val trackingController: String,
    val descentProfile: List<DescentSegment>,
    val basedOnNavdata: Boolean,
    var timeToLooseOrGain: Duration? = null
) : RunwayOccurrence(timelineId, time, runway), Flight

data class DescentSegment(
    val inbound: String,
    val position: LatLng,
    val targetAltitude: Int,
    val remainingDistance: Float,
    val remainingTime: Duration,
    val groundSpeed: Int,
    val tas: Int,
    val wind: Wind,
    val heading: Int
)

data class DescentStep(
    val position: LatLng,
    val altitudeFt: Int,
    val groundSpeed: Int,
    val tas: Int,
    val wind: Wind
)

data class TimelineConfig(
    val id: Long,
    val label: String,
    val targetFixLeft: String,
    val targetFixRight: String?,
    val viaFixes: List<String>,
    val runwayLeft: String? = null,
    val runwayRight: String? = null,
    val airports: List<String>,
)