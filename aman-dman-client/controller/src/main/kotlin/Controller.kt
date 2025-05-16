import entity.TabData
import entity.TimelineData
import org.example.dto.CreateOrUpdateTimelineDto
import org.example.*
import org.example.config.SettingsManager
import org.example.eventHandling.AmanDataListener
import org.example.eventHandling.ViewListener
import org.example.weather.WindApi

class Controller(val model: AmanDataService, val view: AmanDmanMainFrame) : ViewListener, AmanDataListener {
    private var weatherProfile: VerticalWeatherProfile? = null
    private val timelineGroups = mutableListOf<TimelineGroup>()
    private var selectedCallsign: String? = null

    init {
        model.connectToAtcClient()
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
                    runwayLeft = timelineJson.runwayLeft,
                    runwayRight = timelineJson.runwayRight,
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
        timelineGroups.forEach { group ->
            view.getTabByGroupId(group.id)?.let { tab ->
                val relevantData = amanData.filter { occurrence ->
                    group.timelines.any { timeline -> timeline.airportIcao == occurrence.airportIcao }
                }
                tab.updateAmanData(TabData(
                    timelinesData = group.timelines.map { timeline ->
                        TimelineData(
                            timelineId = timeline.title,
                            left = relevantData.filter { it is RunwayArrivalOccurrence && it.runway == timeline.runwayLeft },
                            right = relevantData.filter { it is RunwayArrivalOccurrence && it.runway == timeline.runwayRight }
                        )
                    }
                ))
            }
        }

        if (selectedCallsign != null) {
            val selectedDescentProfile = amanData.filterIsInstance<RunwayArrivalOccurrence>().find { it.callsign == selectedCallsign }
            if (selectedDescentProfile != null) {
                view.descentProfileVisualizationView.setDescentSegments(selectedDescentProfile.descentTrajectory)
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
        )

    override fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto) {
        registerTimeline(
            config.groupId,
            TimelineConfig(
                title = config.title,
                runwayLeft = config.runwayLeft,
                runwayRight = config.runwayRight,
                targetFixesLeft = config.targetFixesLeft,
                targetFixesRight = config.targetFixesRight,
                airportIcao = config.airportIcao
            )
        )
    }


    private fun registerNewTimelineGroup(timelineGroup: TimelineGroup) {
        timelineGroups.add(timelineGroup)
        view.updateTimelineGroups(timelineGroups)
        view.closeTimelineForm()
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
}