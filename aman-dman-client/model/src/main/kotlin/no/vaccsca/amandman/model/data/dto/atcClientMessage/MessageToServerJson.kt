package no.vaccsca.amandman.model.data.dto.atcClientMessage

sealed class MessageToServerJson(
    val type: String
)

data class RegisterFixInboundsMessageJson(
    val requestId: Int,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val destinationAirports: List<String>,
) : MessageToServerJson("requestInboundsForFix")

data class RegisterDeparturesMessageJson(
    val requestId: Int,
    val airportIcao: String,
) : MessageToServerJson("requestOutbounds")

data class UnregisterTimelineMessageJson(
    val requestId: Int,
) : MessageToServerJson("unregisterTimeline")
