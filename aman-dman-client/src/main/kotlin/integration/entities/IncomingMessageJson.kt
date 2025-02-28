package org.example.integration.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TimelineUpdate::class, name = "timelineUpdate"),
)
sealed class IncomingMessageJson

data class TimelineUpdate(
    val timelineId: Long,
    val arrivals: List<TimelineAircraftJson>
) : IncomingMessageJson()
