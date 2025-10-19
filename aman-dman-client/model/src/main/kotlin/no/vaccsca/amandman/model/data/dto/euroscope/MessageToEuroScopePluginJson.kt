package no.vaccsca.amandman.model.data.dto.euroscope


/**
 * Base class for messages sent to the EuroScope bridge plugin via JSON.
 */
sealed class MessageToEuroScopePluginJson(
    val type: String
)

data class RequestArrivalAndDeparturesMessageJson(
    val icao: String,
) : MessageToEuroScopePluginJson("registerAirport")

data class UnregisterTimelineMessageJson(
    val icao: String,
) : MessageToEuroScopePluginJson("unregisterAirport")

data class AssignRunwayMessage(
    val callsign: String,
    val runway: String,
) : MessageToEuroScopePluginJson("assignRunway")