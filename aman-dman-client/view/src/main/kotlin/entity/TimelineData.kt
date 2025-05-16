package entity

import org.example.TimelineOccurrence

data class TimelineData(
    val timelineId: String,
    val left: List<TimelineOccurrence>,
    val right: List<TimelineOccurrence>,
)