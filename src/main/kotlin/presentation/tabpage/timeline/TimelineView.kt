package org.example.presentation.tabpage.timeline

import domain.TimelineConfig
import org.example.state.ApplicationState
import presentation.tabpage.timeline.OverlayView
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import java.time.Instant
import javax.swing.JLayeredPane
import javax.swing.JPanel

class TimelineView(private val state: ApplicationState, private val timelineConfig: TimelineConfig) : JLayeredPane(), ITimelineView {

    private val basePanel = JPanel(GridBagLayout()) // Panel to hold components in a layout
    private val indicatorsPanel = OverlayView(state, timelineConfig, this)

    private val ruler = Ruler(this, state)

    init {
        layout = null // JLayeredPane requires explicit bounds for components
        add(basePanel)
        add(indicatorsPanel)
        setLayer(basePanel, DEFAULT_LAYER)
        setLayer(indicatorsPanel, PALETTE_LAYER)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH // Allow full height expansion
        gbc.weighty = 1.0 // Make components expand vertically

        // Left TrafficSequenceView
        gbc.gridx = 0
        gbc.weightx = 1.0
        basePanel.add(TrafficSequenceView(this, state, TimelineAlignment.RIGHT), gbc)

        // Ruler
        gbc.gridx = 1
        gbc.weightx = 0.2
        basePanel.add(ruler, gbc)

        // Right TrafficSequenceView
        gbc.gridx = 2
        gbc.weightx = 1.0
        basePanel.add(TrafficSequenceView(this, state, TimelineAlignment.LEFT), gbc)


        state.addListener { evt ->
            if (evt.propertyName == "selectedViewEnd" || evt.propertyName == "selectedViewStart" || evt.propertyName == "delaysChanged") {
                repaint()
            }
        }
    }

    override fun calculateYPositionForInstant(instant: Instant): Int {
        val timespanSeconds = state.selectedViewMax.epochSecond - state.selectedViewMin.epochSecond
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        return (height - pixelsPerSecond * (instant.epochSecond - state.selectedViewMin.epochSecond)).toInt()
    }

    override fun getRulerBounds(): Rectangle {
        return ruler.bounds
    }

    override fun doLayout() {
        super.doLayout()
        basePanel.setBounds(0, 0, width, height) // Resize base panel dynamically
        indicatorsPanel.setBounds(0, 0, width, height)
    }
}