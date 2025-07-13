package no.vaccsca.amandman.common.dto

import no.vaccsca.amandman.common.timelineEvent.TimelineEvent

data class TimelineData(
    val timelineId: String,
    val left: List<TimelineEvent>,
    val right: List<TimelineEvent>,
)