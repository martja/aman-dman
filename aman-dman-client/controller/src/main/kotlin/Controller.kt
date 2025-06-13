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

            updateTimeToLose(amanData)
        }
    }

    /**
     * Updates the time to lose (TTL) for all runway arrivals based on their spacing requirements.
     */
    private fun updateTimeToLose(amanData: List<TimelineOccurrence>) {
        val adjustments = mutableMapOf<String, Long>()

        val sortedArrivals = amanData
            .filterIsInstance<RunwayArrivalOccurrence>()
            .sortedBy { it.time }

        if (sortedArrivals.isEmpty()) return

        // Start with the first aircraft's original ETA
        var referenceTime = sortedArrivals.first().time.epochSeconds
        adjustments[sortedArrivals.first().callsign] = 0L // no adjustment for the first

        for (i in 1 until sortedArrivals.size) {
            val leader = sortedArrivals[i - 1]
            val follower = sortedArrivals[i]

            val spacingNm = nmSpacingMap[
                Pair(leader.wakeCategory, follower.wakeCategory)
            ] ?: 3.0

            val requiredDelta = nmToSeconds(spacingNm)

            referenceTime += requiredDelta
            val actualFollowerTime = follower.time.epochSeconds

            val delta = referenceTime - actualFollowerTime

            // Only time to lose — never time to gain
            val timeToLose = if (delta > 0) delta else 0L
            adjustments[follower.callsign] = timeToLose

            // Update reference time to include the actual or delayed follower arrival
            referenceTime = maxOf(referenceTime, actualFollowerTime + timeToLose)
        }

        // Apply adjustments
        amanData.forEach { occurrence ->
            val runwayArrival = occurrence as? RunwayArrivalOccurrence ?: return@forEach
            val adjustment = adjustments[occurrence.callsign] ?: 0L
            runwayArrival.timeToLooseOrGain = adjustment.seconds
        }
    }

    /**
     * A map that defines the required spacing in nautical miles (nm) between aircraft based on their wake turbulence categories.
     * The keys are pairs of wake categories, and the values are the required spacing in nm.
     */
    private val nmSpacingMap = mapOf(
        Pair('H', 'H') to 4.0,
        Pair('H', 'M') to 5.0,
        Pair('H', 'L') to 6.0,
        Pair('M', 'L') to 5.0,
        Pair('J', 'H') to 6.0,
        Pair('J', 'M') to 7.0,
        Pair('J', 'L') to 8.0,
    )

    /**
     * TODO: use estimated ground speed from the aircraft data, both from leading and following aircraft.
     */
    private fun nmToSeconds(distanceNm: Double, groundSpeedKt: Double = 140.0): Long {
        val hours = distanceNm / groundSpeedKt
        return (hours * 3600).toLong()
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