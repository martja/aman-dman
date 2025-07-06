package no.vaccsca.amandman.common.dto

import no.vaccsca.amandman.common.TimelineOccurrence

data class TimelineData(
    val timelineId: String,
    val left: List<TimelineOccurrence>,
    val right: List<TimelineOccurrence>,
)