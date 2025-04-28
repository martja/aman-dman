package tabpage.timeline

import kotlinx.datetime.Clock
import org.example.*
import org.example.eventHandling.ViewListener
import tabpage.timeline.labels.ArrivalLabel
import tabpage.timeline.labels.DepartureLabel
import tabpage.timeline.labels.TimelineLabel
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Polygon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.min

class TimelineOverlay(
    val timelineConfig: TimelineConfig,
    val timelineView: TimelineView,
    val viewListener: ViewListener
) : JPanel(null) {
    private val pointDiameter = 6
    private val scaleMargin = 30
    private val labelWidth = 320

    private val allLabels = hashMapOf<String, TimelineLabel>()

    private var timelineOccurrences: List<TimelineOccurrence> = emptyList()

    private val timelineNameLabel = JLabel(timelineConfig.title, SwingConstants.CENTER).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        background = Color.WHITE
        foreground = Color.BLACK
        isOpaque = true
    }

    init {
        isOpaque = false

        add(timelineNameLabel)
    }

    fun updateTimelineOccurrences(timelineOccurrences: List<TimelineOccurrence>) {
        this.timelineOccurrences = timelineOccurrences
        updateFlightLabels()
        repaint()
    }

    override fun doLayout() {
        super.doLayout()

        updateFlightLabels()

        val scale = timelineView.getScaleBounds()

        val leftSideLabels = allLabels.filter { it.value.timelineOccurrence.isLeftSide() }.values.toList()
        val rightSideLabels = allLabels.filter { !it.value.timelineOccurrence.isLeftSide() }.values.toList()

        rearrangeLabels(leftSideLabels, scale.x - labelWidth - scaleMargin)
        rearrangeLabels(rightSideLabels, scale.x + scaleMargin + scale.width)

        timelineNameLabel.setBounds(scale.x - 10, scale.y + scale.height - 20, 100, 20)
    }

    private fun rearrangeLabels(selectedLabels: List<TimelineLabel>, x: Int) {
        var previousTop: Int? = null
        selectedLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - label.height / 2
            val labelY =
                if (previousTop == null)
                    centerY
                else
                    min(previousTop!! - 3, centerY)

            label.setBounds(x, labelY, labelWidth, 20)
            previousTop = label.y - label.height
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        doLayout()
        val scaleBounds = timelineView.getScaleBounds()

        allLabels.values.forEach {
            val leftSide = it.timelineOccurrence.isLeftSide()

            val dotX = if (leftSide) scaleBounds.x else scaleBounds.x + scaleBounds.width
            val dotY = timelineView.calculateYPositionForInstant(it.getTimelinePlacement())
            val labelX = if (leftSide) it.x + it.width else it.x
            g.drawLine(labelX, it.y + it.height / 2, dotX, dotY)
            g.fillOval(
                dotX - pointDiameter / 2,
                dotY - pointDiameter / 2,
                pointDiameter,
                pointDiameter,
            )
        }

        paintHourglass(g, scaleBounds.x)
        paintHourglass(g, scaleBounds.x + scaleBounds.width)
    }

    private fun updateFlightLabels() {
        val currentCallsigns = timelineOccurrences.mapNotNull { it.getFlight()?.callsign }

        // Remove labels for flights that are no longer in the timeline
        val iterator = allLabels.entries.iterator()
        while (iterator.hasNext()) {
            val (callsign, label) = iterator.next()
            if (callsign !in currentCallsigns) {
                iterator.remove()
                remove(label)
            }
        }

        timelineOccurrences.forEach { timelineOccurrence ->
            val flight = timelineOccurrence.getFlight()
            if (flight != null) {
                val label = allLabels[flight.callsign]
                if (label != null) {
                    label.timelineOccurrence = timelineOccurrence
                    label.updateText()
                } else {
                    val newLabel = timelineOccurrence.createLabel()
                    newLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                    newLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent) {
                            handleLabelClick(newLabel)
                        }
                    })

                    allLabels[flight.callsign] = newLabel
                    add(newLabel)
                }
            }
        }
    }

    private fun handleLabelClick(label: TimelineLabel) {
        val flight = label.timelineOccurrence.getFlight()
        if (flight != null) {
            viewListener.onAircraftSelected(flight.callsign)
        }
    }

    private fun TimelineOccurrence.getFlight(): Flight? {
        return when (this) {
            is FixInboundOccurrence -> this
            is DepartureOccurrence -> this
            is RunwayArrivalOccurrence -> this
            is RunwayDelayOccurrence -> null
        }
    }

    private fun TimelineOccurrence.createLabel(): TimelineLabel {
        return when (this) {
            //is FixInboundOccurrence -> ArrivalLabel(this)
            is DepartureOccurrence -> DepartureLabel(this)
            is RunwayArrivalOccurrence -> ArrivalLabel(this)
            else -> throw IllegalArgumentException("Unsupported occurrence type")
        }
    }

    private fun TimelineOccurrence.isLeftSide(): Boolean =
        when (this) {
            is RunwayArrivalOccurrence -> timelineConfig.runwayLeft == this.runway
            is DepartureOccurrence -> timelineConfig.runwayLeft == this.runway
            is FixInboundOccurrence -> timelineConfig.runwayLeft == this.runway
            is RunwayDelayOccurrence -> timelineConfig.runwayLeft == this.runway
        }

    private fun paintHourglass(g: Graphics, xPosition: Int) {
        val nowY = timelineView.calculateYPositionForInstant(Clock.System.now())
        g.color = Color.WHITE
        val hourglassSize = 6

        g.fillPolygon(Polygon(
            intArrayOf(xPosition, xPosition - hourglassSize, xPosition - hourglassSize),
            intArrayOf(nowY, nowY - hourglassSize, nowY + hourglassSize),
            3
        ))
        g.fillPolygon(Polygon(
            intArrayOf(xPosition, xPosition + hourglassSize, xPosition + hourglassSize),
            intArrayOf(nowY, nowY + hourglassSize, nowY - hourglassSize),
            3
        ))
    }

}

