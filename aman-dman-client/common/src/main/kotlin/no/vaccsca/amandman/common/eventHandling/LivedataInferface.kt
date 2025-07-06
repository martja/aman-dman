package no.vaccsca.amandman.common.eventHandling

import no.vaccsca.amandman.common.TimelineOccurrence

interface LivedataInferface {
    fun onLiveData(amanData: List<TimelineOccurrence>)
}