package atcClient

import kotlinx.datetime.Instant
import org.example.DepartureOccurrence
import org.example.FixInboundOccurrence
import org.example.integration.entities.*

abstract class AtcClient {
    abstract fun sendMessage(message: MessageToServer)

    private val fixInboundCallbacks = mutableMapOf<Int, (List<FixInboundOccurrence>) -> Unit>()
    private val departureCallbacks = mutableMapOf<Int, (List<DepartureOccurrence>) -> Unit>()
    private val arrivalCallbacks = mutableMapOf<Int, (List<ArrivalJson>) -> Unit>()

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
            is ArrivalsUpdate -> {
                arrivalCallbacks[incomingMessageJson.requestId]?.invoke(incomingMessageJson.inbounds)
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

    private fun DepartureJson.toDepartureOccurrence(timelineId: Int) =
        DepartureOccurrence(
            timelineId = timelineId,
            callsign = this.callsign,
            sid = this.sid,
            runway = this.runway,
            icaoType = this.icaoType,
            wakeCategory = this.wakeCategory,
            time = Instant.fromEpochSeconds(this.estimatedDepartureTime),
            airportIcao = this.airportIcao,
        )
}