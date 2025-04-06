package tabpage.timeline

import kotlinx.datetime.Clock
import org.example.*
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
    val timelineView: TimelineView
) : JPanel(null) {
    private val pointDiameter = 6
    private val rulerMargin = 30
    private val labelWidth = 320

    private val allLabels = hashMapOf<String, TimelineLabel>()

    private var timelineOccurrences: List<TimelineOccurrence> = emptyList()

    private val timelineNameLabel = JLabel(timelineConfig.label, SwingConstants.CENTER).apply {
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

        val ruler = timelineView.getRulerBounds()

        val leftSideLabels = allLabels.filter { it.value.timelineOccurrence.isLeftSide() }.values.toList()
        val rightSideLabels = allLabels.filter { !it.value.timelineOccurrence.isLeftSide() }.values.toList()

        rearrangeLabels(leftSideLabels, ruler.x - labelWidth - rulerMargin)
        rearrangeLabels(rightSideLabels, ruler.x + rulerMargin + ruler.width)

        timelineNameLabel.setBounds(ruler.x - 10, ruler.y + ruler.height - 20, 100, 20)
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
        val ruler = timelineView.getRulerBounds()

        allLabels.values.forEach {
            val leftSide = it.timelineOccurrence.isLeftSide()

            val dotX = if (leftSide) ruler.x else ruler.x + ruler.width
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

        paintHourglass(g, ruler.x)
        paintHourglass(g, ruler.x + ruler.width)
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

                    allLabels[flight.callsign] = newLabel
                    add(newLabel)
                }
            }
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
            is RunwayArrivalOccurrence -> this.runway == timelineConfig.runwayLeft
            is DepartureOccurrence -> this.runway == timelineConfig.runwayLeft
            is FixInboundOccurrence -> this.finalFix == timelineConfig.targetFixLeft
            is RunwayDelayOccurrence -> this.runway == timelineConfig.runwayLeft
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

