package org.example.integration

import org.example.integration.entities.MessageToServer
import org.example.integration.entities.RegisterTimeline
import org.example.integration.entities.UnregisterTimeline
import org.example.model.entities.json.RegisterTimelineJson

abstract class AtcClient(
    timelinesToRegister: List<RegisterTimelineJson>
) {
    abstract fun sendMessage(message: MessageToServer)

    private fun setupConnection(timelinesToRegister: List<RegisterTimelineJson>) {
        // Connect to the server and register timelines
    }

    fun registerTimeline(timelineId: Long, targetFixes: List<String>, viaFixes: List<String>, destinationAirports: List<String>) {
        sendMessage(
            RegisterTimeline(
                timelineId = timelineId,
                targetFixes = targetFixes,
                viaFixes = viaFixes,
                destinationAirports = destinationAirports
            )
        )
    }

    fun unregisterTimeline(timelineId: Long) {
        sendMessage(
            UnregisterTimeline(
                timelineId = timelineId
            )
        )
    }
}