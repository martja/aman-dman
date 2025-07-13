package no.vaccsca.amandman.controller

import no.vaccsca.amandman.integration.amanConfig.SettingsManager
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.*
import no.vaccsca.amandman.common.dto.CreateOrUpdateTimelineDto
import no.vaccsca.amandman.common.dto.TabData
import no.vaccsca.amandman.common.dto.TimelineData
import no.vaccsca.amandman.common.eventHandling.LivedataInferface
import no.vaccsca.amandman.common.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.common.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.WeatherDataRepository
import no.vaccsca.amandman.service.AmanDataService
import kotlin.time.Duration.Companion.seconds

class Controller(
    private val service: AmanDataService,
    private val view: ViewInterface,
    private val weatherDataRepository: WeatherDataRepository
) : ControllerInterface, LivedataInferface {

    private var weatherProfile: VerticalWeatherProfile? = null
    private val timelineGroups = mutableListOf<TimelineGroup>()
    private var selectedCallsign: String? = null

    private var timelineConfigs = mutableMapOf<String, TimelineConfig>()

    private val cachedAmanData = mutableMapOf<String, CachedEvent>()
    private val lock = Object()

    init {
        service.livedataInterface = this
        view.controllerInterface = this

        javax.swing.Timer(1000) {
            updateViewFromCachedData()
        }.start()
    }

    override fun onReloadSettingsRequested() {
        timelineGroups.clear()
        timelineConfigs.clear()
        view.updateTimelineGroups(timelineGroups)
        loadSettingsAndOpenTabs()
    }

    private fun loadSettingsAndOpenTabs() {
        SettingsManager.getSettings(reload = true).timelines.forEach { timelineJson ->
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
        Thread {
            val weather = weatherDataRepository.getWindData(lat, lon)
            weatherProfile = weather
            service.updateWeatherData(weather)
            view.updateWeatherData(weather) // This is a call to the interface
        }.start()
    }

    override fun onOpenVerticalProfileWindowClicked() {
        view.openDescentProfileWindow()
    }

    override fun onAircraftSelected(callsign: String) {
        selectedCallsign = callsign
    }

    override fun onLiveData(amanData: List<TimelineEvent>) {
        synchronized(lock) {
            amanData.filterIsInstance<RunwayArrivalEvent>().forEach {
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
            val selectedDescentProfile = snapshot.filterIsInstance<RunwayArrivalEvent>()
                .find { it.callsign == callsign }

            selectedDescentProfile?.let {
                view.updateDescentTrajectory(callsign, it.descentTrajectory)
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

    override fun move(callsign: String, newScheduledTime: Instant) {
        service.suggestScheduledTime(
            callsign,
            newScheduledTime
        )
        view.updateTimelineGroups(timelineGroups)
    }

    override fun onRecalculateSequenceClicked(callSign: String?) {
        service.reSchedule(callSign)
    }

    override fun onRemoveTimelineClicked(timelineConfig: TimelineConfig) {
        timelineGroups.forEach { group ->
            group.timelines.removeIf { it.title == timelineConfig.title }
        }
        view.updateTimelineGroups(timelineGroups)
        // TODO: unsubscribe from inbounds if no timelines left
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
        service.subscribeForInbounds(timelineConfig.airportIcao)
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