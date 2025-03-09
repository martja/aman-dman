package org.example.model

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

data class DescentProfileSegment(
    val minAltitude: Int,
    val maxAltitude: Int,
    val averageHeading: Int,
    val duration: Duration,
    val distance: Float,
)

data class FixInboundOccurrence(
    override val timelineId: Int,
    override val time: Instant,
    override val runway: String,
    override val callsign: String,
    override val icaoType: String,
    override val wakeCategory: Char,
    val assignedStar: String,
    val viaFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val isAboveTransAlt: Boolean,
    val trackedByMe: Boolean,
    val remainingDistance: Float,
    val finalFix: String,
    val finalFixEta: Instant,
    val arrivalAirportIcao: String,
    var timeToLooseOrGain: Duration? = null,
    val descentProfile: List<DescentProfileSegment>,
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
    val viaFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val isAboveTransAlt: Boolean,
    val trackedByMe: Boolean,
    val remainingDistance: Float,
    val finalFix: String,
    val finalFixEta: Instant,
    val arrivalAirportIcao: String,
    var timeToLooseOrGain: Duration? = null
) : RunwayOccurrence(timelineId, time, runway), Flight
