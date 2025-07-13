package no.vaccsca.amandman.model.dto

import no.vaccsca.amandman.model.timelineEvent.TimelineEvent


data class TimelineData(
    val timelineId: String,
    val left: List<TimelineEvent>,
    val right: List<TimelineEvent>,
)