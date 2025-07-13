package no.vaccsca.amandman.common.eventHandling

import no.vaccsca.amandman.common.timelineEvent.TimelineEvent

interface LivedataInferface {
    fun onLiveData(amanData: List<TimelineEvent>)
}