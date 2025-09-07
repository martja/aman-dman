package no.vaccsca.amandman.model.data.dto

sealed class MessageToServer(
    val type: String
)

data class RegisterFixInboundsMessage(
    val requestId: Int,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val destinationAirports: List<String>,
) : MessageToServer("requestInboundsForFix")

data class RegisterDeparturesMessage(
    val requestId: Int,
    val airportIcao: String,
) : MessageToServer("requestOutbounds")

data class UnregisterTimelineMessage(
    val requestId: Int,
) : MessageToServer("unregisterTimeline")
