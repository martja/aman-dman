package tabpage

import entity.TimeRange
import entity.TimelineData
import org.example.*
import org.example.eventHandling.ViewListener
import tabpage.timeline.TimelineView
import util.SharedValue
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane


class TimelineScrollPane(
    val selectedTimeRange: SharedValue<TimeRange>,
    val viewListener: ViewListener
) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    init {
        val items = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL
        viewport.add(items)
    }

    fun insertTimeline(timelineConfig: TimelineConfig) {
        val tl = TimelineView(timelineConfig, selectedTimeRange, viewListener)
        tl.preferredSize = Dimension(800, 0)
        val gbc = GridBagConstraints()
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL
        val items = viewport.view as JPanel
        items.add(tl, gbc)
        items.add(JLabel(), gbc) // Dummy component to force the scrollbars to be left aligned
        items.revalidate()
    }

    fun updateTimelineOccurrences(timelineData: List<TimelineData>) {
        val items = viewport.view as JPanel
        timelineData.forEach {
            val timeline = items.components.find { component ->
                (component as? TimelineView)?.timelineConfig?.title == it.timelineId
            } as? TimelineView
            timeline?.updateTimelineOccurrences(it)
        }
    }

    private fun TimelineConfig.occurrenceIsRelevant(occurrence: TimelineOccurrence): Boolean {
        return when (occurrence) {
            is RunwayArrivalOccurrence ->
                occurrence.runway == runwayLeft || occurrence.runway == runwayRight
            is DepartureOccurrence ->
                occurrence.runway == runwayLeft || occurrence.runway == runwayRight
            is FixInboundOccurrence -> {
                // Check if the route contains any of the target fixes
                val route = occurrence.descentProfile.map { it.fixId }
                targetFixesLeft.any { it in route } || targetFixesRight.any { it in route }
            }
            is RunwayDelayOccurrence -> {
                occurrence.runway == runwayLeft || occurrence.runway == runwayRight
            }
            else -> false
        }
    }
}
