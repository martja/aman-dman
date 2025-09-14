package no.vaccsca.amandman.presenter

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import no.vaccsca.amandman.model.data.dto.TabData

interface ViewInterface {
    var presenterInterface: PresenterInterface

    fun updateTimelineGroups(timelineGroups: List<TimelineGroup>)
    fun openMetWindow()
    fun openLandingRatesWindow()
    fun openNonSequencedWindow()
    fun updateWeatherData(airportIcao: String, weather: VerticalWeatherProfile?)
    fun openDescentProfileWindow()
    fun openTimelineConfigForm(groupId: String, existingConfig: TimelineConfig? = null)
    fun closeTimelineForm()
    fun updateDescentTrajectory(callsign: String, trajectory: List<TrajectoryPoint>)
    fun showTabContextMenu(tabIndex: Int, airportIcao: String)
    fun updateTab(airportIcao: String, tabData: TabData)
    fun removeTab(airportIcao: String)
    fun showTabContextMenu(tabIndex: Int, availableTimelines: List<TimelineConfig>)
    fun updateDraggedLabel(callsign: String, newInstant: Instant, isAvailable: Boolean)
    fun updateRunwayModes(airportIcao: String, runwayModes: List<Pair<String, Boolean>>)
    fun showErrorMessage(message: String)
    fun openWindow()
    fun updateMinimumSpacing(airportIcao: String, minimumSpacingNm: Double)
}