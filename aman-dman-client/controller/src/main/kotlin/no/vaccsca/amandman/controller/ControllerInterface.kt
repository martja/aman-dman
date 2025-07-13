package no.vaccsca.amandman.controller

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.dto.CreateOrUpdateTimelineDto

interface ControllerInterface {
    fun onReloadSettingsRequested()
    fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto)
    fun onOpenMetWindowClicked()
    fun refreshWeatherData(lat: Double, lon: Double)
    fun onOpenVerticalProfileWindowClicked()
    fun onAircraftSelected(callsign: String)
    fun onEditTimelineRequested(groupId: String, timelineTitle: String)
    fun onCreateNewTimelineClicked(groupId: String)
    fun onTabMenu(tabIndex: Int, airportIcao: String)
    fun onNewTimelineGroup(airportIcao: String)
    fun onAddTimelineButtonClicked(airportIcao: String, timelineConfig: TimelineConfig)
    fun onRemoveTab(airportIcao: String)
    fun onOpenLandingRatesWindow()
    fun onOpenNonSequencedWindow()
    fun move(callsign: String, newScheduledTime: Instant)
    fun onRecalculateSequenceClicked(callSign: String? = null)
    fun onRemoveTimelineClicked(timelineConfig: TimelineConfig)
}