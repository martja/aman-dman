import kotlinx.datetime.*
import org.example.dto.CreateOrUpdateTimelineDto
import org.example.*
import org.example.config.SettingsManager
import org.example.dto.TabData
import org.example.dto.TimelineData
import org.example.eventHandling.LivedataInferface
import org.example.weather.WindApi
import kotlin.time.Duration.Companion.seconds

class Controller(val model: AmanDataService, val view: ViewInterface) : ControllerInterface, LivedataInferface {
    private var weatherProfile: VerticalWeatherProfile? = null
    private val timelineGroups = mutableListOf<TimelineGroup>()
    private var selectedCallsign: String? = null

    private var timelineConfigs = mutableMapOf<String, TimelineConfig>()

    private val cachedAmanData = mutableMapOf<String, CachedOccurrence>()
    private val lock = Object()

    init {
        model.connectToAtcClient()

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
            val weather = WindApi().getVerticalProfileAtPoint(lat, lon)
            weatherProfile = weather
            model.updateWeatherData(weather)
            view.updateWeatherData(weather) // This is a call to the interface
        }.start()
    }

    override fun onOpenVerticalProfileWindowClicked() {
        view.openDescentProfileWindow()
    }

    override fun onAircraftSelected(callsign: String) {
        selectedCallsign = callsign
    }

    override fun onLiveData(amanData: List<TimelineOccurrence>) {
        synchronized(lock) {
            amanData.filterIsInstance<RunwayArrivalOccurrence>().forEach {
                cachedAmanData[it.callsign] = CachedOccurrence(
                    lastTimestamp = Clock.System.now(),
                    timelineOccurrence = it
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
        val snapshot: List<TimelineOccurrence>
        synchronized(lock) {
            snapshot = cachedAmanData.values.toList().map { it.timelineOccurrence }
        }

        timelineGroups.forEach { group ->
            val relevantDataForTab = snapshot.filter { occurrence ->
                group.timelines.any { it.airportIcao == occurrence.airportIcao }
            }
            view.updateTab(group.airportIcao, TabData(
                timelinesData = group.timelines.map { timeline ->
                    TimelineData(
                        timelineId = timeline.title,
                        left = relevantDataForTab.filter { it is RunwayArrivalOccurrence && timeline.runwaysLeft.contains(it.runway) },
                        right = relevantDataForTab.filter { it is RunwayArrivalOccurrence && timeline.runwaysRight.contains(it.runway) }
                    )
                }
            ))
        }

        selectedCallsign?.let { callsign ->
            val selectedDescentProfile = snapshot.filterIsInstance<RunwayArrivalOccurrence>()
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
        model.subscribeForInbounds(timelineConfig.airportIcao)
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

    private data class CachedOccurrence(
        val lastTimestamp: Instant,
        val timelineOccurrence: TimelineOccurrence
    )
}