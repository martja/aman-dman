import entity.TimeRange
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import org.example.eventHandling.ViewListener
import tabpage.TimeRangeScrollBar
import tabpage.TimelineScrollPane
import tabpage.TopBar
import util.SharedValue
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TabView(
    private val viewListener: ViewListener,
) : JPanel(BorderLayout()) {

    private var rangeStart: Instant? = null
    private var rangeEnd: Instant? = null

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
    val timelineScrollPane = TimelineScrollPane(selectedTimeRange, viewListener)

    init {
        add(TopBar(), BorderLayout.NORTH)

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

    fun addTimeline(timelineConfig: TimelineConfig) {
        timelineScrollPane.insertTimeline(timelineConfig)
    }

    fun updateAmanData(amanData: List<TimelineOccurrence>) {
        timeWindowScrollbar.setTimelineOccurrences(amanData)
        timelineScrollPane.updateTimelineOccurrences(amanData)
    }
}