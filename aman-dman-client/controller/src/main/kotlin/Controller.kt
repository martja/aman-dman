import entity.TabData
import entity.TimelineData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.example.dto.CreateOrUpdateTimelineDto
import org.example.*
import org.example.config.SettingsManager
import org.example.eventHandling.AmanDataListener
import org.example.eventHandling.ViewListener
import org.example.weather.WindApi
import kotlin.time.Duration.Companion.seconds

class Controller(val model: AmanDataService, val view: AmanDmanMainFrame) : ViewListener, AmanDataListener {
    private var weatherProfile: VerticalWeatherProfile? = null
    private val timelineGroups = mutableListOf<TimelineGroup>()
    private var selectedCallsign: String? = null

    private val cachedAmanData = mutableMapOf<String, CachedOccurrence>()
    private val lock = Object()

    init {
        model.connectToAtcClient()

        javax.swing.Timer(1000) {
            updateViewFromCachedData()
        }.start()
    }

    override fun onLoadAllTabsRequested() {
        SettingsManager.getSettings().timelines.forEach { (id, timelineJson) ->
            registerNewTimelineGroup(
                TimelineGroup(
                    id = id,
                    name = timelineJson.title,
                    timelines = mutableListOf()
                )
            )
            registerTimeline(
                id,
                TimelineConfig(
                    title = timelineJson.title,
                    runwaysLeft = timelineJson.runwaysLeft,
                    runwaysRight = timelineJson.runwaysRight,
                    targetFixesLeft = timelineJson.targetFixesLeft,
                    targetFixesRight = timelineJson.targetFixesRight,
                    airportIcao = timelineJson.airportIcao
                )
            )
        }

        view.updateTimelineGroups(timelineGroups)
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
            view.getTabByGroupId(group.id)?.let { tab ->
                val relevantDataForTab = snapshot.filter { occurrence ->
                    group.timelines.any { it.airportIcao == occurrence.airportIcao }
                }

                tab.updateAmanData(TabData(
                    timelinesData = group.timelines.map { timeline ->
                        TimelineData(
                            timelineId = timeline.title,
                            left = relevantDataForTab.filter { it is RunwayArrivalOccurrence && timeline.runwaysLeft.contains(it.runway) },
                            right = relevantDataForTab.filter { it is RunwayArrivalOccurrence && timeline.runwaysRight.contains(it.runway) }
                        )
                    }
                ))
            }
        }

        selectedCallsign?.let { callsign ->
            val selectedDescentProfile = snapshot.filterIsInstance<RunwayArrivalOccurrence>()
                .find { it.callsign == callsign }

            selectedDescentProfile?.let {
                view.descentProfileVisualizationView.setDescentSegments(it.descentTrajectory)
            }
        }
    }

    override fun onNewTimelineGroup(title: String) =
        registerNewTimelineGroup(
            TimelineGroup(
                id = title,
                name = title,
                timelines = mutableListOf()
            )
        ).also {
            view.closeTimelineForm()
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
        timelineGroups.add(timelineGroup)
        view.updateTimelineGroups(timelineGroups)
    }

    private fun registerTimeline(groupId: String, timelineConfig: TimelineConfig) {
        val group = timelineGroups.find { it.id == groupId }
        if (group != null) {
            group.timelines += timelineConfig
            view.updateTimelineGroups(timelineGroups)
            view.closeTimelineForm()
        }
        model.subscribeForInbounds(timelineConfig.airportIcao)
    }

    override fun onEditTimelineRequested(groupId: String, timelineTitle: String) {
        val group = timelineGroups.find { it.id == groupId }
        if (group != null) {
            val existingConfig = group.timelines.find { it.title == timelineTitle }
            if (existingConfig != null) {
                view.openTimelineConfigForm(groupId, existingConfig)
            }
        }
    }

    override fun onNewTimelineClicked(groupId: String) {
        view.openTimelineConfigForm(groupId)
    }

    private data class CachedOccurrence(
        val lastTimestamp: Instant,
        val timelineOccurrence: TimelineOccurrence
    )
}