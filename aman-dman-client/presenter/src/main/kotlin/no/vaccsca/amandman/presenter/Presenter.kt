package no.vaccsca.amandman.presenter

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.common.TimelineGroup
import no.vaccsca.amandman.model.ApplicationMode
import no.vaccsca.amandman.model.data.dto.CreateOrUpdateTimelineDto
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.model.data.service.PlannerService
import no.vaccsca.amandman.model.domain.exception.UnsupportedInSlaveModeException
import no.vaccsca.amandman.model.domain.service.DataUpdateListener
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.time.Duration.Companion.seconds

class Presenter(
    private val plannerService: PlannerService,
    private val view: ViewInterface,
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
        SettingsRepository.getSettings(reload = true).timelines.forEach { timelineJson ->
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

    override fun refreshWeatherData(airportIcao: String, lat: Double, lon: Double) {
        plannerService.refreshWeatherData(airportIcao, lat, lon)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to refresh weather data")
                }
            }
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

    override fun onLiveData(airportIcao: String, timelineEvents: List<TimelineEvent>) {
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

    override fun onMinimumSpacingUpdated(airportIcao: String, minimumSpacingNm: Double) {
        this.minimumSpacingNm = minimumSpacingNm
        runwayModeStateManager.updateMinimumSpacing(this.minimumSpacingNm)
        view.updateMinimumSpacing(airportIcao, minimumSpacingNm)
    }

    override fun onWeatherDataUpdated(airportIcao: String, data: VerticalWeatherProfile?) {
        view.updateWeatherData(airportIcao, data)
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

        selectedCallsign?.let { callsign ->
            if (applicationMode == ApplicationMode.SLAVE) return
            plannerService.getDescentProfileForCallsign(callsign)
                .onSuccess { selectedDescentProfile ->
                    if (selectedDescentProfile != null)
                        view.updateDescentTrajectory(callsign, selectedDescentProfile)
                }
                .onFailure {
                    when (it) {
                        is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                        else -> view.showErrorMessage("Failed to fetch descent profile")
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
        plannerService.suggestScheduledTime(sequenceId, callsign, newScheduledTime)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to move aircraft")
                }
            }
    }

    override fun onRecalculateSequenceClicked(sequenceId: String, callSign: String?) {
        plannerService.reSchedule(sequenceId, callSign)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to re-schedule")
                }
            }
    }

    override fun setMinimumSpacingDistance(airportIcao: String, minimumSpacingDistanceNm: Double) {
        plannerService.setMinimumSpacing(airportIcao, minimumSpacingDistanceNm)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> {
                        view.showErrorMessage(it.msg)
                    }
                    else -> view.showErrorMessage("Failed to set minimum spacing")
                }
            }
    }

    override fun onLabelDragged(sequenceId: String, callsign: String, newInstant: Instant) {
        plannerService.isTimeSlotAvailable(sequenceId, callsign, newInstant)
            .onSuccess { view.updateDraggedLabel(callsign, newInstant, it) }
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to check time slot availability")
                }
            }
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
        plannerService.planArrivalsFor(timelineConfig.airportIcao)
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