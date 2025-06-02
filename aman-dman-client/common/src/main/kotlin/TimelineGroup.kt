package org.example

data class TimelineGroup(
    val airportIcao: String,
    val name: String,
    val timelines: MutableList<TimelineConfig>
)