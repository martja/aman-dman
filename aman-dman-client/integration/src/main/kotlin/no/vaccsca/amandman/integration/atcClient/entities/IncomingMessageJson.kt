package no.vaccsca.amandman.integration.atcClient.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArrivalsUpdate::class, name = "arrivals"),
    JsonSubTypes.Type(value = DeparturesUpdate::class, name = "departures"),
    JsonSubTypes.Type(value = RunwayStatusesUpdate::class, name = "runwayStatuses")
)
sealed class IncomingMessageJson(
    open val requestId: Int,
)

data class DeparturesUpdate(
    override val requestId: Int,
    val outbounds: List<DepartureJson>
) : IncomingMessageJson(requestId)

data class ArrivalsUpdate(
    override val requestId: Int,
    val inbounds: List<ArrivalJson>
) : IncomingMessageJson(requestId)

data class RunwayStatusesUpdate(
    override val requestId: Int,
    val airports: Map<String, Map<String, RunwayStatus>>
) : IncomingMessageJson(requestId)