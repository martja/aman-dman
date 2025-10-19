package no.vaccsca.amandman.view.tabpage.timeline

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.util.SharedValue
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.JLayeredPane
import javax.swing.JPanel
import kotlin.time.Duration.Companion.seconds

class TimelineView(
    val timelineConfig: TimelineConfig,
    private val selectedTimeRange: SharedValue<TimeRange>,
    private val presenterInterface: PresenterInterface,
) : JLayeredPane() {
    private val basePanel = JPanel(GridBagLayout()) // Panel to hold components in a layout
    private val labelContainer = TimelineOverlay(timelineConfig, this, presenterInterface)
    private val isDual = timelineConfig.runwaysLeft.isNotEmpty() && timelineConfig.runwaysRight.isNotEmpty()
    private val timeScale = TimeScale(this, selectedTimeRange, !isDual, presenterInterface)

    init {
        layout = null // JLayeredPane requires explicit bounds for components
        add(basePanel)
        add(labelContainer)
        setLayer(basePanel, DEFAULT_LAYER)
        setLayer(labelContainer, PALETTE_LAYER)

        val scaleWidth = 60
        val listWidth = 280
        val totalTimelineWidth =
            if (isDual) scaleWidth + listWidth * 2
            else scaleWidth + listWidth

        preferredSize = Dimension(totalTimelineWidth, 0)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH // Allow full height expansion
        gbc.weighty = 1.0 // Make components expand vertically

        if (isDual) {
            // Left TrafficSequenceView
            gbc.gridx = 0
            gbc.weightx = listWidth / totalTimelineWidth.toDouble()
            basePanel.add(SequenceStack(this, TimelineAlignment.RIGHT), gbc)

            // Scale visualisation
            gbc.gridx = 1
            gbc.weightx = scaleWidth / totalTimelineWidth.toDouble()
            basePanel.add(timeScale, gbc)

            // Right TrafficSequenceView
            gbc.gridx = 2
            gbc.weightx = listWidth / totalTimelineWidth.toDouble()
            basePanel.add(SequenceStack(this, TimelineAlignment.LEFT), gbc)
        } else {
            // Scale visualisation
            gbc.gridx = 0
            gbc.weightx = scaleWidth / totalTimelineWidth.toDouble()
            basePanel.add(timeScale, gbc)

            // Single TrafficSequenceView
            gbc.gridx = 1
            gbc.weightx = listWidth / totalTimelineWidth.toDouble()
            basePanel.add(SequenceStack(this, TimelineAlignment.RIGHT), gbc)
        }

        selectedTimeRange.addListener {
            labelContainer.repaint()
        }
    }

    fun updateTimelineData(timelineData: TimelineData) {
        labelContainer.updateTimelineData(timelineData)
        timeScale.updateTimelineData(timelineData)
    }

    fun calculateYPositionForInstant(instant: Instant): Int {
        val timespanSeconds = selectedTimeRange.value.end.epochSeconds - selectedTimeRange.value.start.epochSeconds
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        return (height - pixelsPerSecond * (instant.epochSeconds - selectedTimeRange.value.start.epochSeconds)).toInt()
    }

    fun calculateInstantForYPosition(y: Int): Instant {
        val timespanSeconds = selectedTimeRange.value.end.epochSeconds - selectedTimeRange.value.start.epochSeconds
        val pixelsPerSecond = height.toFloat() / timespanSeconds.toFloat()
        val secondsFromStart = (height - y) / pixelsPerSecond
        return selectedTimeRange.value.start + secondsFromStart.toLong().seconds
    }

    fun getScaleBounds(): Rectangle {
        return timeScale.bounds
    }

    override fun doLayout() {
        super.doLayout()
        basePanel.setBounds(0, 0, width, height) // Resize base panel dynamically
        labelContainer.setBounds(0, 0, width, height)
    }

    fun updateDraggedLabel(timelineEvent: TimelineEvent, proposedTime: Instant, available: Boolean) {
        labelContainer.updateDraggedLabel(timelineEvent, proposedTime, available)
    }
}