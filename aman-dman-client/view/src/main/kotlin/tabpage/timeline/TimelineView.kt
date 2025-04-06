package tabpage.timeline

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.JLayeredPane
import javax.swing.JPanel

class TimelineView(timelineConfig: TimelineConfig) : JLayeredPane() {
    private val basePanel = JPanel(GridBagLayout()) // Panel to hold components in a layout
    private val labelContainer = TimelineOverlay(timelineConfig, this)

    private var selectedViewMin: Instant = Clock.System.now()
    private var selectedViewMax: Instant = Clock.System.now()

    private val ruler = Ruler(this)

    init {
        layout = null // JLayeredPane requires explicit bounds for components
        add(basePanel)
        add(labelContainer)
        setLayer(basePanel, DEFAULT_LAYER)
        setLayer(labelContainer, PALETTE_LAYER)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH // Allow full height expansion
        gbc.weighty = 1.0 // Make components expand vertically

        // Left TrafficSequenceView
        gbc.gridx = 0
        gbc.weightx = 1.0
        basePanel.add(TrafficSequenceView(this, TimelineAlignment.RIGHT), gbc)

        // Ruler
        gbc.gridx = 1
        gbc.weightx = 0.2
        basePanel.add(ruler, gbc)

        // Right TrafficSequenceView
        gbc.gridx = 2
        gbc.weightx = 1.0
        basePanel.add(TrafficSequenceView(this, TimelineAlignment.LEFT), gbc)
    }

    fun updateTimelineOccurrences(occurrences: List<TimelineOccurrence>) {
        for (i in 0 until basePanel.componentCount) {
            val component = basePanel.getComponent(i)
            if (component is TrafficSequenceView) {
                component.updateTimelineOccurrences(occurrences)
            }
        }
        labelContainer.updateTimelineOccurrences(occurrences)
        ruler.updateTimelineOccurrences(occurrences)
    }

    fun updateSelectedViewRange(start: Instant, end: Instant) {
        selectedViewMin = start
        selectedViewMax = end
        ruler.updateSelectedViewRange(start, end)
        repaint()
    }

    fun calculateYPositionForInstant(instant: Instant): Int {
        val timespanSeconds = selectedViewMax.epochSeconds - selectedViewMin.epochSeconds
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        return (height - pixelsPerSecond * (instant.epochSeconds - selectedViewMin.epochSeconds)).toInt()
    }

    fun getRulerBounds(): Rectangle {
        return ruler.bounds
    }

    override fun doLayout() {
        super.doLayout()
        basePanel.setBounds(0, 0, width, height) // Resize base panel dynamically
        labelContainer.setBounds(0, 0, width, height)
    }
}