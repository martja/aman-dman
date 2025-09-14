package no.vaccsca.amandman.presenter

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.data.dto.CreateOrUpdateTimelineDto

interface PresenterInterface {
    fun onReloadSettingsRequested()
    fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto)
    fun onOpenMetWindowClicked()
    fun refreshWeatherData(airportIcao: String, lat: Double, lon: Double)
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
    fun move(sequenceId: String, callsign: String, newScheduledTime: Instant)
    fun onRecalculateSequenceClicked(sequenceId: String, callSign: String? = null)
    fun onRemoveTimelineClicked(timelineConfig: TimelineConfig)
    fun onLabelDragged(sequenceId: String, callsign: String, newInstant: Instant)
    fun setMinimumSpacingDistance(airportIcao: String, minimumSpacingDistanceNm: Double)
}