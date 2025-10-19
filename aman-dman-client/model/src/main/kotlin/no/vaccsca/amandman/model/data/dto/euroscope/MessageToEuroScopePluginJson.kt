package no.vaccsca.amandman.model.data.dto.euroscope


/**
 * Base class for messages sent to the EuroScope bridge plugin via JSON.
 */
sealed class MessageToEuroScopePluginJson(
    val type: String
)

data class RegisterFixInboundsMessageJson(
    val requestId: Int,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val destinationAirports: List<String>,
) : MessageToEuroScopePluginJson("requestInboundsForFix")

data class RegisterDeparturesMessageJson(
    val requestId: Int,
    val airportIcao: String,
) : MessageToEuroScopePluginJson("requestOutbounds")

data class UnregisterTimelineMessageJson(
    val requestId: Int,
) : MessageToEuroScopePluginJson("cancelRequest")

data class AssignRunwayMessage(
    val requestId: Int,
    val callsign: String,
    val runway: String,
) : MessageToEuroScopePluginJson("assignRunway")