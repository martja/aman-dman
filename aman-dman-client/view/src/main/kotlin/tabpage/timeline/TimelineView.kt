package tabpage.timeline

import entity.TimeRange
import org.example.dto.TimelineData
import kotlinx.datetime.Instant
import org.example.TimelineConfig
import ControllerInterface
import util.SharedValue
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.JLayeredPane
import javax.swing.JPanel

class TimelineView(
    val timelineConfig: TimelineConfig,
    private val selectedTimeRange: SharedValue<TimeRange>,
    private val controllerInterface: ControllerInterface,
) : JLayeredPane() {
    private val basePanel = JPanel(GridBagLayout()) // Panel to hold components in a layout
    private val palettePanel = TimelineOverlay(timelineConfig, this, controllerInterface)
    private val isDual = timelineConfig.runwaysLeft.isNotEmpty() && timelineConfig.runwaysRight.isNotEmpty()
    private val timeScale = TimeScale(this, selectedTimeRange, !isDual)

    init {
        layout = null // JLayeredPane requires explicit bounds for components
        add(basePanel)
        add(palettePanel)
        setLayer(basePanel, DEFAULT_LAYER)
        setLayer(palettePanel, PALETTE_LAYER)


        preferredSize = Dimension(if (isDual) 580 else 240, 0)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH // Allow full height expansion
        gbc.weighty = 1.0 // Make components expand vertically

        if (isDual) {
            // Left TrafficSequenceView
            gbc.gridx = 0
            gbc.weightx = 1.0
            basePanel.add(SequenceStack(this, TimelineAlignment.RIGHT), gbc)

            // Scale visualisation
            gbc.gridx = 1
            gbc.weightx = 0.25
            basePanel.add(timeScale, gbc)

            // Right TrafficSequenceView
            gbc.gridx = 2
            gbc.weightx = 1.0
            basePanel.add(SequenceStack(this, TimelineAlignment.LEFT), gbc)
        } else {
            // Scale visualisation
            gbc.gridx = 0
            gbc.weightx = 0.2
            basePanel.add(timeScale, gbc)

            // Single TrafficSequenceView
            gbc.gridx = 1
            gbc.weightx = 1.0
            basePanel.add(SequenceStack(this, TimelineAlignment.RIGHT), gbc)
        }

        selectedTimeRange.addListener {
            palettePanel.repaint()
        }
    }

    fun updateTimelineData(timelineData: TimelineData) {
        palettePanel.updateTimelineData(timelineData)
        timeScale.updateTimelineData(timelineData)
    }

    fun calculateYPositionForInstant(instant: Instant): Int {
        val timespanSeconds = selectedTimeRange.value.end.epochSeconds - selectedTimeRange.value.start.epochSeconds
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        return (height - pixelsPerSecond * (instant.epochSeconds - selectedTimeRange.value.start.epochSeconds)).toInt()
    }

    fun getScaleBounds(): Rectangle {
        return timeScale.bounds
    }

    override fun doLayout() {
        super.doLayout()
        basePanel.setBounds(0, 0, width, height) // Resize base panel dynamically
        palettePanel.setBounds(0, 0, width, height)
    }
}