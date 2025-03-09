package org.example.integration

import kotlinx.datetime.Instant
import org.example.integration.entities.*
import org.example.model.DepartureOccurrence
import org.example.model.DescentProfileSegment
import org.example.model.FixInboundOccurrence
import kotlin.time.Duration.Companion.seconds

abstract class AtcClient {
    abstract fun sendMessage(message: MessageToServer)

    private val fixInboundCallbacks = mutableMapOf<Int, (List<FixInboundOccurrence>) -> Unit>()
    private val departureCallbacks = mutableMapOf<Int, (List<DepartureOccurrence>) -> Unit>()

    private var nextRequestId = 0
        get() {
            return field++
        }

    fun collectInboundsForFix(
        targetFixes: List<String>,
        viaFixes: List<String>,
        destinationAirports: List<String>,
        onDataReceived: (List<FixInboundOccurrence>) -> Unit
    ) {
        val timelineId = nextRequestId
        fixInboundCallbacks[timelineId] = onDataReceived
        sendMessage(
            RegisterFixInboundsMessage(
                requestId = timelineId,
                targetFixes = targetFixes,
                viaFixes = viaFixes,
                destinationAirports = destinationAirports
            )
        )
    }

    fun collectDeparturesFrom(
        airportIcao: String,
        onDataReceived: (List<DepartureOccurrence>) -> Unit
    ) {
        val timelineId = nextRequestId
        departureCallbacks[timelineId] = onDataReceived
        sendMessage(
            RegisterDeparturesMessage(
                requestId = timelineId,
                airportIcao = airportIcao,
            )
        )
    }

    protected fun handleMessage(incomingMessageJson: IncomingMessageJson) {
        when (incomingMessageJson) {
            is FixInboundsUpdate -> {
                val arrivals = incomingMessageJson.inbounds.map { it.toFixInboundOccurrence(incomingMessageJson.requestId) }
                fixInboundCallbacks[incomingMessageJson.requestId]?.invoke(arrivals)
            }
            is DeparturesUpdate -> {
                val departures = incomingMessageJson.outbounds.map { it.toDepartureOccurrence(incomingMessageJson.requestId) }
                departureCallbacks[incomingMessageJson.requestId]?.invoke(departures)
            }
        }
    }

    fun unregisterTimeline(timelineId: Int) {
        sendMessage(
            UnregisterTimelineMessage(
                requestId = timelineId
            )
        )
    }

    private fun FixInboundJson.toFixInboundOccurrence(timelineId: Int) =
        FixInboundOccurrence(
            timelineId = timelineId,
            callsign = this.callsign,
            icaoType = this.icaoType,
            wakeCategory =  this.wtc,
            runway = this.runway,
            assignedStar =  this.star,
            time = Instant.fromEpochSeconds(this.eta),
            remainingDistance = this.remainingDist,
            finalFix = this.finalFix,
            flightLevel = this.flightLevel,
            pressureAltitude = this.pressureAltitude,
            groundSpeed = this.groundSpeed,
            isAboveTransAlt = this.isAboveTransAlt,
            trackedByMe = this.trackedByMe,
            arrivalAirportIcao = "N/A",
            viaFix = this.viaFix,
            finalFixEta = Instant.fromEpochSeconds(this.finalFixEta),
            descentProfile = this.descentProfile.map {
                DescentProfileSegment(
                    minAltitude = it.minAltitude,
                    maxAltitude = it.maxAltitude,
                    averageHeading = it.averageHeading,
                    duration = it.secDuration.seconds,
                    distance = it.distance
                )
            }
        )

    private fun DepartureJson.toDepartureOccurrence(timelineId: Int) =
        DepartureOccurrence(
            timelineId = timelineId,
            callsign = this.callsign,
            sid = this.sid,
            runway = this.runway,
            icaoType = this.icaoType,
            wakeCategory = this.wakeCategory,
            time = Instant.fromEpochSeconds(this.estimatedDepartureTime),
        )
}