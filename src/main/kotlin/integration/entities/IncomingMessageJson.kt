package org.example.integration.entities

sealed class IncomingMessageJson(
    val type: String
)

data class TimelineUpdate(
    val timelineId: Long,
    val timelineData: List<TimelineAircraftJson>
) : IncomingMessageJson("timelineUpdate")