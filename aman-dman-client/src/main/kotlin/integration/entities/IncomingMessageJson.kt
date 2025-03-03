package org.example.integration.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TimelineUpdate::class, name = "timelineUpdate"),
    JsonSubTypes.Type(value = DmanUpdate::class, name = "departuresUpdate"),
)
sealed class IncomingMessageJson

data class TimelineUpdate(
    val timelineId: Long,
    val arrivals: List<TimelineAircraftJson>
) : IncomingMessageJson()

data class DmanUpdate(
    val timelineId: Long,
    val departures: List<DmanAircraftJson>
) : IncomingMessageJson()