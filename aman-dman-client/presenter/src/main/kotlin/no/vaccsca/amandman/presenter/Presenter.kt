package no.vaccsca.amandman.presenter

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.service.DataUpdateListener
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.common.TimelineGroup
import no.vaccsca.amandman.model.AmanModel
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.ApplicationMode
import no.vaccsca.amandman.model.data.dto.CreateOrUpdateTimelineDto
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.model.data.dto.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.data.dto.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import no.vaccsca.amandman.model.data.service.AmanPlannerService
import kotlin.time.Duration.Companion.seconds

class Presenter(
    private val plannerService: AmanPlannerService?,
    private val view: ViewInterface,
    private val model: AmanModel,
    override val applicationMode: ApplicationMode,
) : ModeAwarePresenterInterface, DataUpdateListener {

    private val timelineGroups = mutableListOf<TimelineGroup>()
    private var selectedCallsign: String? = null

    private var timelineConfigs = mutableMapOf<String, TimelineConfig>()

    private val cachedAmanData = mutableMapOf<String, CachedEvent>()
    private val lock = Object()

    // Replace the simple currentMinimumSpacingNm with a reactive state manager
    private val runwayModeStateManager = RunwayModeStateManager(view)
    private var minimumSpacingNm: Double = 0.0

    init {
        view.presenterInterface = this

        javax.swing.Timer(1000) {
            updateViewFromCachedData()
        }.start()
    }

    override fun onReloadSettingsRequested() {
        timelineGroups.clear()
        timelineConfigs.clear()
        view.updateTimelineGroups(timelineGroups)
        runwayModeStateManager.refreshAllStates() // Refresh runway mode states when settings change
        loadSettingsAndOpenTabs()
    }

    private fun loadSettingsAndOpenTabs() {
        model.getSettings(reload = true).timelines.forEach { timelineJson ->
            val newTimelineConfig = TimelineConfig(
                title = timelineJson.title,
                runwaysLeft = timelineJson.runwaysLeft,
                runwaysRight = timelineJson.runwaysRight,
                targetFixesLeft = timelineJson.targetFixesLeft,
                targetFixesRight = timelineJson.targetFixesRight,
                airportIcao = timelineJson.airportIcao
            )
            registerNewTimelineGroup(
                TimelineGroup(
                    airportIcao = timelineJson.airportIcao,
                    name = timelineJson.airportIcao,
                    timelines = mutableListOf()
                )
            )
            timelineConfigs[timelineJson.title] = newTimelineConfig
        }
    }

    override fun onOpenMetWindowClicked() {
        view.openMetWindow()
    }

    override fun refreshWeatherData(lat: Double, lon: Double) {
        plannerService?.refreshWeatherData(lat, lon)
    }

    override fun onOpenVerticalProfileWindowClicked() {
        if (!isFeatureAvailable(Feature.VIEW_DESCENT_PROFILE)) {
            view.showErrorMessage("Descent profile view not available in slave mode")
            return
        }
        view.openDescentProfileWindow()
    }

    override fun onAircraftSelected(callsign: String) {
        selectedCallsign = callsign
    }

    override fun onLiveData(timelineEvents: List<TimelineEvent>) {
        synchronized(lock) {
            timelineEvents.filterIsInstance<RunwayArrivalEvent>().forEach {
                cachedAmanData[it.callsign] = CachedEvent(
                    lastTimestamp = Clock.System.now(),
                    timelineEvent = it
                )
            }

            // Delete stale data
            val cutoffTime = Clock.System.now() - 5.seconds
            cachedAmanData.entries.removeIf { entry ->
                entry.value.lastTimestamp < cutoffTime
            }
        }
        updateViewFromCachedData()
    }

    override fun onRunwayModesUpdated(
        airportIcao: String,
        runwayStatuses: Map<String, RunwayStatus>,
    ) {
        runwayModeStateManager.updateRunwayStatuses(airportIcao, runwayStatuses, minimumSpacingNm)
    }

    override fun onMinimumSpacingUpdated(minimumSpacingNm: Double) {
        this.minimumSpacingNm = minimumSpacingNm
        runwayModeStateManager.updateMinimumSpacing(this.minimumSpacingNm)
    }

    override fun onWeatherDataUpdated(data: VerticalWeatherProfile?) {
        view.updateWeatherData(data)
    }

    private fun updateViewFromCachedData() {
        val snapshot: List<TimelineEvent>
        synchronized(lock) {
            snapshot = cachedAmanData.values.toList().map { it.timelineEvent }
        }

        timelineGroups.forEach { group ->
            val relevantDataForTab = snapshot.filter { occurrence ->
                group.timelines.any { it.airportIcao == occurrence.airportIcao }
            }
            view.updateTab(group.airportIcao, TabData(
                timelinesData = group.timelines.map { timeline ->
                    TimelineData(
                        timelineId = timeline.title,
                        left = relevantDataForTab.filter { it is RunwayArrivalEvent && timeline.runwaysLeft.contains(it.runway) },
                        right = relevantDataForTab.filter { it is RunwayArrivalEvent && timeline.runwaysRight.contains(it.runway) }
                    )
                }
            ))
        }

        if (plannerService != null) {
            selectedCallsign?.let { callsign ->
                val selectedDescentProfile = plannerService.getDescentProfileForCallsign(callsign)
                selectedDescentProfile?.let {
                    view.updateDescentTrajectory(callsign, selectedDescentProfile)
                }
            }
        }
    }

    override fun onTabMenu(tabIndex: Int, airportIcao: String) {
        val availableTimelinesForIcao =
            timelineConfigs.values
                .filter { it.airportIcao == airportIcao }
                .toSet()
                .toList()

        view.showTabContextMenu(tabIndex, availableTimelinesForIcao)
    }

    override fun onNewTimelineGroup(airportIcao: String) =
        registerNewTimelineGroup(
            TimelineGroup(
                airportIcao = airportIcao,
                name = airportIcao,
                timelines = mutableListOf()
            )
        ).also {
            view.closeTimelineForm()
        }

    override fun onAddTimelineButtonClicked(airportIcao: String, timelineConfig: TimelineConfig) {
        registerTimeline(
            airportIcao,
            timelineConfig
        )
    }

    override fun onRemoveTab(airportIcao: String) {
        view.removeTab(airportIcao)
        timelineGroups.removeAll { it.airportIcao == airportIcao }
    }

    override fun onOpenLandingRatesWindow() {
        view.openLandingRatesWindow()
    }

    override fun onOpenNonSequencedWindow() {
        view.openNonSequencedWindow()
    }

    override fun move(sequenceId: String, callsign: String, newScheduledTime: Instant) {
        if (!isFeatureAvailable(Feature.MANUAL_AIRCRAFT_MOVEMENT)) {
            view.showErrorMessage("Manual aircraft movement not available in slave mode")
            return
        }
        plannerService?.suggestScheduledTime(sequenceId, callsign, newScheduledTime)
    }

    override fun onRecalculateSequenceClicked(sequenceId: String, callSign: String?) {
        if (!isFeatureAvailable(Feature.RECALCULATE_SEQUENCE)) {
            view.showErrorMessage("Sequence recalculation not available in slave mode")
            return
        }
        plannerService?.reSchedule(sequenceId, callSign)
    }

    override fun setMinimumSpacingDistance(minimumSpacingDistanceNm: Double) {
        if (!isFeatureAvailable(Feature.SET_MINIMUM_SPACING)) {
            view.showErrorMessage("Minimum spacing adjustment not available in slave mode")
            return
        }
        plannerService?.setMinimumSpacing(minimumSpacingDistanceNm)
    }

    override fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto) {
        registerTimeline(
            config.groupId,
            TimelineConfig(
                title = config.title,
                runwaysLeft = config.left.targetRunways,
                runwaysRight = config.right.targetRunways,
                targetFixesLeft = config.left.targetFixes,
                targetFixesRight = config.right.targetFixes,
                airportIcao = config.airportIcao
            )
        )
    }

    override fun onRemoveTimelineClicked(timelineConfig: TimelineConfig) {
        if (!isFeatureAvailable(Feature.DELETE_TIMELINE)) {
            view.showErrorMessage("Timeline deletion not available in slave mode")
            return
        }
        timelineGroups.forEach { group ->
            group.timelines.removeIf { it.title == timelineConfig.title }
        }
        view.updateTimelineGroups(timelineGroups)
        // TODO: unsubscribe from inbounds if no timelines left
    }

    override fun onLabelDragged(sequenceId: String, callsign: String, newInstant: Instant) {
        if (!isFeatureAvailable(Feature.MANUAL_AIRCRAFT_MOVEMENT)) {
            view.showErrorMessage("Manual aircraft movement not available in slave mode")
            return
        }
        // Check if the new scheduled time is available
        if (plannerService == null) {
            return
        }
        val isAvailable = plannerService.isTimeSlotAvailable(sequenceId, callsign, newInstant)
        view.updateDraggedLabel(callsign, newInstant, isAvailable)
    }

    // Remove the old withModeCheck helper function as it's no longer needed

    private fun registerNewTimelineGroup(timelineGroup: TimelineGroup) {
        if (timelineGroups.any { it.airportIcao == timelineGroup.airportIcao }) {
            return // Group already exists
        }

        timelineGroups.add(timelineGroup)
        view.updateTimelineGroups(timelineGroups)
    }

    private fun registerTimeline(groupId: String, timelineConfig: TimelineConfig) {
        val group = timelineGroups.find { it.airportIcao == groupId }
        if (group != null) {
            group.timelines += timelineConfig
            view.updateTimelineGroups(timelineGroups)
            view.closeTimelineForm()
        }
        plannerService?.subscribeForInbounds(timelineConfig.airportIcao)
    }

    override fun onEditTimelineRequested(groupId: String, timelineTitle: String) {
        val group = timelineGroups.find { it.airportIcao == groupId }
        if (group != null) {
            val existingConfig = group.timelines.find { it.title == timelineTitle }
            if (existingConfig != null) {
                view.openTimelineConfigForm(groupId, existingConfig)
            }
        }
    }

    override fun onCreateNewTimelineClicked(groupId: String) {
        view.openTimelineConfigForm(groupId)
    }

    private data class CachedEvent(
        val lastTimestamp: Instant,
        val timelineEvent: TimelineEvent
    )
}