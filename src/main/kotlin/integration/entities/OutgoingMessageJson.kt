package org.example.integration.entities

sealed class MessageToServer(
    val type: String
)

data class RegisterTimeline(
    val timelineId: Long,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val destinationAirports: List<String>,
) : MessageToServer("registerTimeline")

data class UnregisterTimeline(
    val timelineId: Long,
) : MessageToServer("unregisterTimeline")
