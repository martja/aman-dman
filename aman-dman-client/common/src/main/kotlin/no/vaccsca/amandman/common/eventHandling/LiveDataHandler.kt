package no.vaccsca.amandman.common.eventHandling

import no.vaccsca.amandman.common.timelineEvent.TimelineEvent

interface LiveDataHandler {
    fun onLiveData(amanData: List<TimelineEvent>)
}