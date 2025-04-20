package org.example.eventHandling

import org.example.dto.CreateOrUpdateTimelineDto

interface ViewListener {
    fun onLoadAllTabsRequested()
    fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto)
    fun onOpenMetWindowClicked()
    fun refreshWeatherData(lat: Double, lon: Double)
    fun onOpenVerticalProfileWindowClicked()
    fun onAircraftSelected(callsign: String)
    fun onEditTimelineRequested(groupId: String, timelineTitle: String)
    fun onNewTimelineClicked(groupId: String)
    fun onNewTimelineGroup(title: String)
}