package org.example.presentation.tabpage.timeline

import kotlinx.datetime.Instant
import model.entities.TimelineConfig
import org.example.controller.TimelineController
import org.example.model.TimelineState
import view.tabpage.timeline.OverlayView
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.JLayeredPane
import javax.swing.JPanel

class TimelineView(
    private val timelineState: TimelineState,
    private val timelineController: TimelineController,
    private val timelineConfig: TimelineConfig
) : JLayeredPane(), ITimelineView {

    private val basePanel = JPanel(GridBagLayout()) // Panel to hold components in a layout
    private val indicatorsPanel = OverlayView(timelineState, timelineConfig, this)

    private val ruler = Ruler(this, timelineState)

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
        basePanel.add(TrafficSequenceView(this, timelineState, TimelineAlignment.RIGHT), gbc)

        // Ruler
        gbc.gridx = 1
        gbc.weightx = 0.2
        basePanel.add(ruler, gbc)

        // Right TrafficSequenceView
        gbc.gridx = 2
        gbc.weightx = 1.0
        basePanel.add(TrafficSequenceView(this, timelineState, TimelineAlignment.LEFT), gbc)

        timelineState.addListener { evt ->
            if (evt.propertyName == "selectedViewEnd" || evt.propertyName == "selectedViewStart" || evt.propertyName == "delaysChanged") {
                repaint()
            }
        }
    }

    override fun calculateYPositionForInstant(instant: Instant): Int {
        val timespanSeconds = timelineState.selectedViewMax.epochSeconds - timelineState.selectedViewMin.epochSeconds
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        return (height - pixelsPerSecond * (instant.epochSeconds - timelineState.selectedViewMin.epochSeconds)).toInt()
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