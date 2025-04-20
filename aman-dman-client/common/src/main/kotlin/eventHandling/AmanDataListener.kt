package org.example.eventHandling

import org.example.TimelineOccurrence

interface AmanDataListener {
    fun onLiveData(amanData: List<TimelineOccurrence>)
}