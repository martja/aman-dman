import kotlinx.datetime.Instant
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import tabpage.TimeRangeScrollBar
import tabpage.TimelineScrollPane
import tabpage.TopBar
import java.awt.BorderLayout
import javax.swing.JPanel

class TabView : JPanel(BorderLayout()) {

    private var rangeStart: Instant? = null
    private var rangeEnd: Instant? = null

    val timeWindowScrollbar = TimeRangeScrollBar(::handleRangeChange)
    val timelineScrollPane = TimelineScrollPane()

    init {
        add(TopBar(), BorderLayout.NORTH)

        add(timeWindowScrollbar, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)
    }

    fun addTimeline(timelineConfig: TimelineConfig) {
        timelineScrollPane.insertTimeline(timelineConfig)
    }

    fun handleRangeChange(start: Instant, end: Instant) {
        rangeStart = start
        rangeEnd = end

        timeWindowScrollbar.updateRange(start, end)
        timelineScrollPane.updateRange(start, end)
        timelineScrollPane.repaint()
    }

    fun updateAmanData(amanData: List<TimelineOccurrence>) {
        timeWindowScrollbar.setTimelineOccurrences(amanData)
        timelineScrollPane.updateTimelineOccurrences(amanData)
    }
}