package no.vaccsca.amandman.integration.atcClient

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.timelineEvent.DepartureEvent
import no.vaccsca.amandman.common.timelineEvent.FixInboundEvent
import no.vaccsca.amandman.integration.atcClient.entities.ArrivalJson
import no.vaccsca.amandman.integration.atcClient.entities.ArrivalsUpdate
import no.vaccsca.amandman.integration.atcClient.entities.DepartureJson
import no.vaccsca.amandman.integration.atcClient.entities.DeparturesUpdate
import no.vaccsca.amandman.integration.atcClient.entities.IncomingMessageJson
import no.vaccsca.amandman.integration.atcClient.entities.MessageToServer
import no.vaccsca.amandman.integration.atcClient.entities.RegisterDeparturesMessage
import no.vaccsca.amandman.integration.atcClient.entities.RegisterFixInboundsMessage
import no.vaccsca.amandman.integration.atcClient.entities.UnregisterTimelineMessage

abstract class AtcClient {
    abstract fun sendMessage(message: MessageToServer)

    private val fixInboundCallbacks = mutableMapOf<Int, (List<FixInboundEvent>) -> Unit>()
    private val departureCallbacks = mutableMapOf<Int, (List<DepartureEvent>) -> Unit>()
    private val arrivalCallbacks = mutableMapOf<Int, (List<ArrivalJson>) -> Unit>()

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
        onDataReceived: (List<ArrivalJson>) -> Unit
    ) {
        val timelineId = nextRequestId
        arrivalCallbacks[timelineId] = onDataReceived
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
            is ArrivalsUpdate -> {
                arrivalCallbacks[incomingMessageJson.requestId]?.invoke(incomingMessageJson.inbounds)
            }
            is DeparturesUpdate -> {
                val departures = incomingMessageJson.outbounds.map { it.toDepartureEvent(incomingMessageJson.requestId) }
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
}