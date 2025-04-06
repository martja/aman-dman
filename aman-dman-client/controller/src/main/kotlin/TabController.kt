/*
import model.entities.TimelineConfig
import org.example.config.SettingsManager
import org.example.integration.AtcClient
import org.example.model.TabState
import org.example.model.TimelineOccurrence
import org.example.model.TimelineState
import org.example.state.ApplicationState
import org.example.view.TabView
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TabController(
    private val applicationState: ApplicationState,
    private val tabState: TabState,
    private val atcClient: AtcClient,
    private val mainController: MainController,
) {
    private var tabView: TabView? = null
    val MIN_RANGE: Duration = 10.minutes

    private val timelineStates = mutableListOf<TimelineState>()

    init {
        applicationState.addListener { event ->
            if (event.propertyName == "timeNow") {
                moveTimeRange(1.seconds)
            }
        }
    }

    fun moveTimeRange(delta: Duration) {
        val newMin = tabState.selectedViewMin.plus(delta)
        val newMax = tabState.selectedViewMax.plus(delta)

        if (newMin > tabState.timelineMinTime && newMax < tabState.timelineMaxTime) {
            tabState.selectedViewMin = newMin
            tabState.selectedViewMax = newMax
        }
    }

    fun moveTimeRangeStart(delta: Duration) {
        val newStartTime = tabState.selectedViewMin.plus(delta)

        // Ensure the new start time is within bounds
        if (newStartTime > tabState.timelineMinTime && newStartTime < tabState.selectedViewMax.minus(MIN_RANGE)) {
            tabState.selectedViewMin = newStartTime
        }
    }

    fun moveTimeRangeEnd(delta: Duration) {
        val newEndTime = tabState.selectedViewMax.plus(delta)

        // Ensure the new start time is within bounds
        if (newEndTime < tabState.timelineMaxTime && newEndTime > tabState.selectedViewMin.plus(MIN_RANGE)) {
            tabState.selectedViewMax = newEndTime
        }
    }

    fun openNewTimeline(id: String) {
        SettingsManager.getSettings().timelines.get(id)?.let { timelineJson ->
            val timelineConfig =  TimelineConfig(
                id = id.hashCode().toLong(),
                label = id,
                targetFixLeft = timelineJson.targetFixes.first(),
                targetFixRight = timelineJson.targetFixes.last(),
                viaFixes = timelineJson.viaFixes,
                airports = timelineJson.destinationAirports,
                runwayLeft = "01L",
                runwayRight = "01R",
            )
            val timelineState = TimelineState(tabState, timelineConfig)
            val timelineController = TimelineController(timelineState, atcClient, timelineConfig, mainController)

            tabView?.addTimeline(timelineConfig, timelineState)
            timelineStates.add(timelineState)
        }
    }

    fun getAllTimelineOccurrences(): List<TimelineOccurrence> {
        return timelineStates.flatMap { it.timelineOccurrences }
    }

    fun setView(tabView: TabView) {
        this.tabView = tabView
        openNewTimeline("GM 19R/19L")
        openNewTimeline("GM 01L/01R")
        //openNewTimeline("VALPU | INSUV")
    }
}*/