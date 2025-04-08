package tabpage.timeline

import entity.TimeRange
import kotlinx.datetime.Instant
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import org.example.eventHandling.ViewListener
import util.SharedValue
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.JLayeredPane
import javax.swing.JPanel

class TimelineView(
    private val timelineConfig: TimelineConfig,
    private val selectedTimeRange: SharedValue<TimeRange>,
    private val viewListener: ViewListener,
) : JLayeredPane() {
    private val basePanel = JPanel(GridBagLayout()) // Panel to hold components in a layout
    private val palettePanel = TimelineOverlay(timelineConfig, this, viewListener)

    private val ruler = Ruler(this, selectedTimeRange)

    init {
        layout = null // JLayeredPane requires explicit bounds for components
        add(basePanel)
        add(palettePanel)
        setLayer(basePanel, DEFAULT_LAYER)
        setLayer(palettePanel, PALETTE_LAYER)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH // Allow full height expansion
        gbc.weighty = 1.0 // Make components expand vertically

        // Left TrafficSequenceView
        gbc.gridx = 0
        gbc.weightx = 1.0
        basePanel.add(SequenceStack(this, TimelineAlignment.RIGHT), gbc)

        // Ruler
        gbc.gridx = 1
        gbc.weightx = 0.2
        basePanel.add(ruler, gbc)

        // Right TrafficSequenceView
        gbc.gridx = 2
        gbc.weightx = 1.0
        basePanel.add(SequenceStack(this, TimelineAlignment.LEFT), gbc)

        selectedTimeRange.addListener {
            palettePanel.repaint()
        }
    }

    fun updateTimelineOccurrences(occurrences: List<TimelineOccurrence>) {
        palettePanel.updateTimelineOccurrences(occurrences)
        ruler.updateTimelineOccurrences(occurrences)
    }

    fun calculateYPositionForInstant(instant: Instant): Int {
        val timespanSeconds = selectedTimeRange.value.end.epochSeconds - selectedTimeRange.value.start.epochSeconds
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        return (height - pixelsPerSecond * (instant.epochSeconds - selectedTimeRange.value.start.epochSeconds)).toInt()
    }

    fun getRulerBounds(): Rectangle {
        return ruler.bounds
    }

    override fun doLayout() {
        super.doLayout()
        basePanel.setBounds(0, 0, width, height) // Resize base panel dynamically
        palettePanel.setBounds(0, 0, width, height)
    }
}