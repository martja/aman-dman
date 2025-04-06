package org.example.eventHandling

import org.example.TimelineOccurrence

interface AmanDataListener {
    fun onNewAmanData(amanData: List<TimelineOccurrence>)
}