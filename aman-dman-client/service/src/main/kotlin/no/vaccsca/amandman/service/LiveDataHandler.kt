package no.vaccsca.amandman.service

import no.vaccsca.amandman.model.timelineEvent.TimelineEvent

interface LiveDataHandler {
    fun onLiveData(amanData: List<TimelineEvent>)
}