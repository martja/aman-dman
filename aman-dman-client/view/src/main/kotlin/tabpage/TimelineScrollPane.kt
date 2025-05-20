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
        val items = viewport.view as JPanel

        // Remove the previous glue (assumes it’s always the last component and a JLabel)
        if (items.componentCount > 0) {
            val last = items.getComponent(items.componentCount - 1)
            if (last is JLabel) {
                items.remove(last)
            }
        }

        val gbc = GridBagConstraints().apply {
            gridx = items.componentCount
            weightx = 0.0
            weighty = 1.0
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.VERTICAL
        }
        items.add(tl, gbc)

        // Add new glue at the end
        val glue = JLabel()
        val glueConstraints = GridBagConstraints().apply {
            gridx = items.componentCount
            weightx = 1.0
            weighty = 0.0
            fill = GridBagConstraints.BOTH
        }
        items.add(glue, glueConstraints)

        items.revalidate()
        items.repaint()
    }


    fun updateTimelineOccurrences(timelineData: List<TimelineData>) {
        val items = viewport.view as JPanel
        timelineData.forEach {
           items.components.filterIsInstance<TimelineView>().forEach { timelineView ->
                if (timelineView.timelineConfig.title == it.timelineId) {
                    timelineView.updateTimelineData(it)
                }
            }
        }
    }
}
