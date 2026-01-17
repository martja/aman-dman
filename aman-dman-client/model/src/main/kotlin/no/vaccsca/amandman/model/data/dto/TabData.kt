package no.vaccsca.amandman.model.data.dto

import no.vaccsca.amandman.model.domain.valueobjects.NonSequencedEvent
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData

data class TabData(
    val timelinesData: List<TimelineData>,
    val nonSequencedList: List<NonSequencedEvent>,
)