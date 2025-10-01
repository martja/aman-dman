package no.vaccsca.amandman.presenter

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.model.UserRole
import no.vaccsca.amandman.model.data.dto.CreateOrUpdateTimelineDto
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.data.repository.NavdataRepository
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.model.data.repository.WeatherDataRepository
import no.vaccsca.amandman.model.domain.PlannerManager
import no.vaccsca.amandman.model.domain.service.PlannerServiceMaster
import no.vaccsca.amandman.model.domain.service.PlannerServiceSlave
import no.vaccsca.amandman.model.data.integration.AtcClientEuroScope
import no.vaccsca.amandman.model.data.integration.SharedStateHttpClient
import no.vaccsca.amandman.model.domain.exception.UnsupportedInSlaveModeException
import no.vaccsca.amandman.model.domain.service.DataUpdateListener
import no.vaccsca.amandman.model.domain.service.DataUpdatesServerSender
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.time.Duration.Companion.seconds

class Presenter(
    private val plannerManager: PlannerManager,
    private val view: ViewInterface,
    private val guiUpdater: DataUpdateListener,
) : PresenterInterface, DataUpdateListener {

    private val timelineGroups = mutableListOf<TimelineGroup>()
    private var selectedCallsign: String? = null

    private var timelineConfigs = mutableMapOf<String, TimelineConfig>()

    private val cachedAmanData = mutableMapOf<String, CachedEvent>()
    private val lock = Object()

    // Replace the simple currentMinimumSpacingNm with a reactive state manager
    private val runwayModeStateManager = RunwayModeStateManager(view)
    private var minimumSpacingNm: Double = 0.0

    // Create or return existing AtcClientEuroScope instance
    private var euroScopeClient: AtcClientEuroScope? = null

    private fun getOrCreateAtcClient(): AtcClientEuroScope {
        if (euroScopeClient == null || !euroScopeClient!!.isClientConnected) {
            euroScopeClient = AtcClientEuroScope()
        }
        return euroScopeClient!!
    }

    private val dataUpdatesServerSender by lazy {
        DataUpdatesServerSender()
    }

    private val sharedStateHttpClient by lazy {
        SharedStateHttpClient()
    }

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
            timelineConfigs[timelineJson.title] = newTimelineConfig
        }
    }

    override fun onOpenMetWindowClicked() {
        view.openMetWindow()
    }

    override fun refreshWeatherData(airportIcao: String) {
        plannerManager.getServiceForAirport(airportIcao).refreshWeatherData()
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to refresh weather data")
                }
            }
    }

    override fun onOpenVerticalProfileWindowClicked() {
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
                        left = relevantDataForTab.filter { it is RunwayArrivalEvent && timeline.runwaysLeft.contains(it.runway.id) },
                        right = relevantDataForTab.filter { it is RunwayArrivalEvent && timeline.runwaysRight.contains(it.runway.id) }
                    )
                }
            ))
        }

        selectedCallsign?.let { callsign ->
           /* plannerManager.getAllServices().getDescentProfileForCallsign(callsign)
                .onSuccess { selectedDescentProfile ->
                    if (selectedDescentProfile != null)
                        view.updateDescentTrajectory(callsign, selectedDescentProfile)
                }
                .onFailure {
                    when (it) {
                        is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                        else -> view.showErrorMessage("Failed to fetch descent profile")
                    }
                }*/
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

    override fun onNewTimelineGroup(airportIcao: String, userRole: UserRole) =
        registerNewTimelineGroup(
            TimelineGroup(
                airportIcao = airportIcao,
                name = airportIcao,
                timelines = mutableListOf(),
                userRole = userRole
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
        // Clean up cached data for this airport
        synchronized(lock) {
            cachedAmanData.entries.removeIf { entry ->
                entry.value.timelineEvent.airportIcao == airportIcao
            }
        }

        // Clear selected callsign if it belongs to the removed airport
        selectedCallsign?.let { callsign ->
            val selectedEvent = synchronized(lock) {
                cachedAmanData[callsign]?.timelineEvent
            }
            if (selectedEvent?.airportIcao == airportIcao) {
                selectedCallsign = null
            }
        }

        // Clean up runway mode state for this airport
        runwayModeStateManager.cleanupAirportState(airportIcao)

        // Get the service before unregistering to check if it's a Master service
        val serviceToRemove = plannerManager.getServiceForAirport(airportIcao)

        // Remove from view and unregister service (this calls service.stop())
        view.removeTab(airportIcao)
        plannerManager.unregisterService(airportIcao)
        timelineGroups.removeAll { it.airportIcao == airportIcao }

        // Check if we need to clean up the shared AtcClient
        // Only close it if no more Master/Local services are using it
        if (serviceToRemove is PlannerServiceMaster) {
            val remainingMasterServices = plannerManager.getAllServices()
                .filterIsInstance<PlannerServiceMaster>()

            if (remainingMasterServices.isEmpty()) {
                // No more services using the AtcClient, clean it up
                euroScopeClient?.close()
                euroScopeClient = null
            }
        }
    }

    override fun onOpenLandingRatesWindow() {
        view.openLandingRatesWindow()
    }

    override fun onOpenNonSequencedWindow() {
        view.openNonSequencedWindow()
    }

    override fun move(airportIcao: String, callsign: String, newScheduledTime: Instant) {
        plannerManager.getServiceForAirport(airportIcao).suggestScheduledTime(callsign, newScheduledTime)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to move aircraft")
                }
            }
    }

    override fun onRecalculateSequenceClicked(airportIcao: String, callSign: String?) {
        plannerManager.getServiceForAirport(airportIcao).reSchedule(callSign)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> view.showErrorMessage(it.msg)
                    else -> view.showErrorMessage("Failed to re-schedule")
                }
            }
    }

    override fun setMinimumSpacingDistance(airportIcao: String, minimumSpacingDistanceNm: Double) {
        plannerManager.getServiceForAirport(airportIcao).setMinimumSpacing(minimumSpacingDistanceNm)
            .onFailure {
                when (it) {
                    is UnsupportedInSlaveModeException -> {
                        view.showErrorMessage(it.msg)
                    }
                    else -> view.showErrorMessage("Failed to set minimum spacing")
                }
            }
    }

    override fun onLabelDragged(airportIcao: String, callsign: String, newInstant: Instant) {
        plannerManager.getServiceForAirport(airportIcao).isTimeSlotAvailable(callsign, newInstant)
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
            config.airportIcao,
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
        timelineGroups.forEach { group ->
            group.timelines.removeIf { it.title == timelineConfig.title }
        }
        view.updateTimelineGroups(timelineGroups)
        // TODO: unsubscribe from inbounds if no timelines left
    }

    private fun registerNewTimelineGroup(timelineGroup: TimelineGroup) {
        if (timelineGroups.any { it.airportIcao == timelineGroup.airportIcao }) {
            return // Group already exists
        }

        val airport = NavdataRepository().airports.find { it.icao == timelineGroup.airportIcao }

        if (airport == null) {
            view.showErrorMessage("Airport ${timelineGroup.airportIcao} not found in navdata")
            return
        }

        val plannerService = when(timelineGroup.userRole) {
            UserRole.MASTER ->
                PlannerServiceMaster(
                    airport = airport,
                    weatherDataRepository = WeatherDataRepository(),
                    atcClient = getOrCreateAtcClient(),
                    dataUpdateListeners = arrayOf(guiUpdater, dataUpdatesServerSender),
                )
            UserRole.LOCAL ->
                PlannerServiceMaster(
                    airport = airport,
                    weatherDataRepository = WeatherDataRepository(),
                    atcClient = getOrCreateAtcClient(),
                    dataUpdateListeners = arrayOf(guiUpdater),
                )
            UserRole.SLAVE ->
                PlannerServiceSlave(
                    airportIcao = timelineGroup.airportIcao,
                    sharedStateHttpClient = sharedStateHttpClient,
                    dataUpdateListener = guiUpdater,
                )
        }

        plannerManager.registerService(plannerService)
        plannerManager.getServiceForAirport(timelineGroup.airportIcao).planArrivals()
        timelineGroups.add(timelineGroup)
        view.updateTimelineGroups(timelineGroups)
    }

    private fun registerTimeline(airportIcao: String, timelineConfig: TimelineConfig) {
        val group = timelineGroups.find { it.airportIcao == airportIcao }
        if (group != null) {
            group.timelines += timelineConfig
            view.updateTimelineGroups(timelineGroups)
            view.closeTimelineForm()
        }
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