package no.vaccsca.amandman.presenter

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.UserRole
import no.vaccsca.amandman.model.data.dto.CreateOrUpdateTimelineDto
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent

/**
 * Interface defining the contract for the Presenter in the MVP architecture.
 * It handles user interactions and communicates with the View and Model layers.
 *
 * View -> Presenter communication
 */
interface PresenterInterface {
    fun onReloadSettingsRequested()
    fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto)
    fun onOpenMetWindowClicked()
    fun onOpenVerticalProfileWindowClicked()
    fun onAircraftSelected(callsign: String)
    fun onEditTimelineRequested(groupId: String, timelineTitle: String)
    fun onCreateNewTimelineClicked(groupId: String)
    fun onTabMenu(tabIndex: Int, airportIcao: String)
    fun onNewTimelineGroup(airportIcao: String, userRole: UserRole)
    fun onAddTimelineButtonClicked(airportIcao: String, timelineConfig: TimelineConfig)
    fun onRemoveTab(airportIcao: String)
    fun onOpenLandingRatesWindow()
    fun onOpenNonSequencedWindow()
    fun onLabelDragEnd(airportIcao: String, timelineEvent: TimelineEvent, newScheduledTime: Instant, newRunway: String? = null)
    fun onRecalculateSequenceClicked(airportIcao: String, callSign: String? = null)
    fun onRemoveTimelineClicked(timelineConfig: TimelineConfig)
    fun onLabelDrag(airportIcao: String, timelineEvent: TimelineEvent, newInstant: Instant)
    fun onMinimumSpacingDistanceSet(airportIcao: String, minimumSpacingDistanceNm: Double)
    fun beginRunwaySelection(runwayEvent: RunwayEvent, onClose: (runway: String?) -> Unit)
}