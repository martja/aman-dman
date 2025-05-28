package org.example.eventHandling

import org.example.TimelineOccurrence

interface LivedataInferface {
    fun onLiveData(amanData: List<TimelineOccurrence>)
}