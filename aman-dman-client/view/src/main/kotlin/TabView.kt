import org.example.dto.TabData
import entity.TimeRange
import kotlinx.datetime.Clock
import org.example.TimelineGroup
import tabpage.TimeRangeScrollBar
import tabpage.TimelineScrollPane
import tabpage.TopBar
import tabpage.timeline.TimelineView
import util.SharedValue
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TabView(
    controller: ControllerInterface,
    val airportIcao: String,
) : JPanel(BorderLayout()) {

    private val availableTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 1.hours,
            Clock.System.now() + 3.hours,
        )
    )

    private val selectedTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 10.minutes,
            Clock.System.now() + 60.minutes,
        )
    )

    val timeWindowScrollbar = TimeRangeScrollBar(selectedTimeRange, availableTimeRange)
    val timelineScrollPane = TimelineScrollPane(selectedTimeRange, controller)

    init {
        add(TopBar(controller), BorderLayout.NORTH)

        add(timeWindowScrollbar, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)

        val timer = Timer(1000) {
            selectedTimeRange.value = TimeRange(
                selectedTimeRange.value.start + 1.seconds,
                selectedTimeRange.value.end + 1.seconds,
            )
        }

        timer.start()
    }

    fun updateAmanData(tabData: TabData) {
        timeWindowScrollbar.setTimelineOccurrences(tabData.timelinesData)
        timelineScrollPane.updateTimelineOccurrences(tabData.timelinesData)
    }

    fun updateTimelines(timelineGroup: TimelineGroup) {
        // Clear existing timelines
        val items = timelineScrollPane.viewport.view as JPanel
        items.components.forEach { component ->
            if (component is TimelineView) {
                items.remove(component)
            }
        }
        // Add the current timelines
        timelineGroup.timelines.forEach { timelineConfig ->
            timelineScrollPane.insertTimeline(timelineConfig)
        }
    }
}