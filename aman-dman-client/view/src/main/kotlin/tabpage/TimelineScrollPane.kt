package tabpage

import entity.TimeRange
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import tabpage.timeline.TimelineView
import util.SharedValue
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane


class TimelineScrollPane(val selectedTimeRange: SharedValue<TimeRange>) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
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
        val tl = TimelineView(timelineConfig, selectedTimeRange)
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

    fun updateTimelineOccurrences(occurrences: List<TimelineOccurrence>) {
        val items = viewport.view as JPanel
        items.components.forEach { component ->
            if (component is TimelineView) {
                component.updateTimelineOccurrences(occurrences)
            }
        }
    }
}
