package no.vaccsca.amandman.model.data.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.data.dto.ArrivalJson
import no.vaccsca.amandman.model.data.dto.ArrivalsUpdateJson
import no.vaccsca.amandman.model.data.dto.DepartureJson
import no.vaccsca.amandman.model.data.dto.DeparturesUpdateJson
import no.vaccsca.amandman.model.data.dto.IncomingMessageJson
import no.vaccsca.amandman.model.data.dto.MessageToServer
import no.vaccsca.amandman.model.data.dto.RegisterDeparturesMessage
import no.vaccsca.amandman.model.data.dto.RegisterFixInboundsMessage
import no.vaccsca.amandman.model.data.dto.RunwayStatusJson
import no.vaccsca.amandman.model.data.dto.RunwayStatusesUpdateJson
import no.vaccsca.amandman.model.data.dto.UnregisterTimelineMessage
import no.vaccsca.amandman.model.data.dto.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.data.dto.timelineEvent.FixInboundEvent

abstract class AmanDataClient {
    abstract fun sendMessage(message: MessageToServer)

    private val fixInboundCallbacks = mutableMapOf<Int, (List<FixInboundEvent>) -> Unit>()
    private val departureCallbacks = mutableMapOf<Int, (List<DepartureEvent>) -> Unit>()
    private val arrivalCallbacks = mutableMapOf<Int, (List<ArrivalJson>) -> Unit>()
    private val runwayStatusCallbacks = mutableMapOf<Int, (Map<String, Map<String, RunwayStatus>>) -> Unit>()

    private var nextRequestId = 0
        get() {
            return field++
        }

    fun collectInboundsForFix(
        targetFixes: List<String>,
        viaFixes: List<String>,
        destinationAirports: List<String>,
        onDataReceived: (List<FixInboundEvent>) -> Unit
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

    fun collectArrivalsFor(
        airportIcao: String,
        onRunwayModesChanged: (Map<String, Map<String, RunwayStatus>>) -> Unit,
        onDataReceived: (List<ArrivalJson>) -> Unit,
    ) {
        val timelineId = nextRequestId
        arrivalCallbacks[timelineId] = onDataReceived
        runwayStatusCallbacks[timelineId] = onRunwayModesChanged
        sendMessage(
            RegisterFixInboundsMessage(
                requestId = timelineId,
                targetFixes = emptyList(),
                viaFixes = emptyList(), // TODO
                destinationAirports = listOf(airportIcao)
            )
        )
    }

    fun collectDeparturesFrom(
        airportIcao: String,
        onDataReceived: (List<DepartureEvent>) -> Unit
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
            is ArrivalsUpdateJson -> {
                arrivalCallbacks[incomingMessageJson.requestId]?.invoke(incomingMessageJson.inbounds)
            }
            is DeparturesUpdateJson -> {
                val departures = incomingMessageJson.outbounds.map { it.toDepartureEvent(incomingMessageJson.requestId) }
                departureCallbacks[incomingMessageJson.requestId]?.invoke(departures)
            }
            is RunwayStatusesUpdateJson -> {
                val statuses = incomingMessageJson.airports.mapValues { entry -> entry.value.mapValues { it.value.toRunwayStatus() } }
                runwayStatusCallbacks[incomingMessageJson.requestId]?.invoke(statuses)
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

    private fun DepartureJson.toDepartureEvent(timelineId: Int) =
        DepartureEvent(
            timelineId = timelineId,
            callsign = this.callsign,
            sid = this.sid,
            runway = this.runway,
            icaoType = this.icaoType,
            wakeCategory = this.wakeCategory,
            scheduledTime = Instant.fromEpochSeconds(this.estimatedDepartureTime),
            estimatedTime = Instant.fromEpochSeconds(this.estimatedDepartureTime),
            airportIcao = this.airportIcao,
        )

    private fun RunwayStatusJson.toRunwayStatus() =
        RunwayStatus(
            departures = this.departures,
            arrivals = this.arrivals,
        )
}