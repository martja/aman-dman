package org.example

data class TimelineGroup(
    val id: String,
    val name: String,
    val timelines: MutableList<TimelineConfig>
)