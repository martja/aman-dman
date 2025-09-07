package no.vaccsca.amandman.model.domain.valueobjects

import no.vaccsca.amandman.model.data.dto.timelineEvent.TimelineEvent


data class TimelineData(
    val timelineId: String,
    val left: List<TimelineEvent>,
    val right: List<TimelineEvent>,
)