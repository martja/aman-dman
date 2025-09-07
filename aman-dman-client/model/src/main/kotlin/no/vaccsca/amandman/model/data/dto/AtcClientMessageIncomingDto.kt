package no.vaccsca.amandman.model.data.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArrivalsUpdateJson::class, name = "arrivals"),
    JsonSubTypes.Type(value = DeparturesUpdateJson::class, name = "departures"),
    JsonSubTypes.Type(value = RunwayStatusesUpdateJson::class, name = "runwayStatuses")
)
sealed class IncomingMessageJson(
    open val requestId: Int,
)

data class DeparturesUpdateJson(
    override val requestId: Int,
    val outbounds: List<DepartureJson>
) : IncomingMessageJson(requestId)

data class ArrivalsUpdateJson(
    override val requestId: Int,
    val inbounds: List<ArrivalJson>
) : IncomingMessageJson(requestId)

data class RunwayStatusesUpdateJson(
    override val requestId: Int,
    val airports: Map<String, Map<String, RunwayStatusJson>>
) : IncomingMessageJson(requestId)

data class RunwayStatusJson(
    val arrivals: Boolean,
    val departures: Boolean
)

data class DepartureJson(
    val callsign: String,
    val icaoType: String,
    val wakeCategory: Char,
    val estimatedDepartureTime: Long,
    val airportIcao: String,
    val runway: String,
    val sid: String,
)

data class ArrivalJson(
    val callsign: String,
    val icaoType: String,
    val assignedRunway: String?,
    val assignedStar: String?,
    val assignedDirect: String?,
    val trackingController: String?,
    val scratchPad: String?,
    val latitude: Double,
    val longitude: Double,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val track: Int,
    val route: List<FixPointJson>,
    val arrivalAirportIcao: String,
    val flightPlanTas: Int?,
)

data class FixPointJson(
    val name: String,
    val isOnStar: Boolean,
    val latitude: Double,
    val longitude: Double,
    val isPassed: Boolean,
)
