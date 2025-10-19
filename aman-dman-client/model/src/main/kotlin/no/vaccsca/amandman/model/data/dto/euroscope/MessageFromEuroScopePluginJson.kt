package no.vaccsca.amandman.model.data.dto.euroscope

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Base class for messages received from the EuroScope plugin.
 * The `type` property is used to determine the specific subclass.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArrivalsUpdateFromServerJson::class, name = "arrivals"),
    JsonSubTypes.Type(value = DeparturesUpdateFromServerJson::class, name = "departures"),
    JsonSubTypes.Type(value = RunwayStatusesUpdateFromServerJson::class, name = "runwayStatuses"),
    JsonSubTypes.Type(value = ControllerInfoFromServerJson::class, name = "controllerInfo"),
)
sealed class MessageFromServerJson(
    open val requestId: Int,
)

data class DeparturesUpdateFromServerJson(
    override val requestId: Int,
    val outbounds: List<DepartureJson>
) : MessageFromServerJson(requestId)

data class ArrivalsUpdateFromServerJson(
    override val requestId: Int,
    val inbounds: List<ArrivalJson>
) : MessageFromServerJson(requestId)

data class RunwayStatusesUpdateFromServerJson(
    override val requestId: Int,
    val airports: Map<String, Map<String, RunwayStatusJson>>
) : MessageFromServerJson(requestId)

data class ControllerInfoFromServerJson(
    override val requestId: Int,
    val me: ControllerInfoJson
) : MessageFromServerJson(requestId)

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
    val latitude: Double,
    val longitude: Double,
    val isPassed: Boolean,
)

data class ControllerInfoJson(
    val positionId: String?,
    val callsign: String?,
    val facilityType: Int?,
)