package view.tabpage.timeline

import model.entities.TimelineConfig
import org.example.model.TimelineState
import org.example.presentation.tabpage.timeline.ITimelineView
import org.example.state.Arrival
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Polygon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

class OverlayView(
    val timelineState: TimelineState,
    val timelineConfig: TimelineConfig,
    val timelineView: ITimelineView
) : JPanel(null) {
    private val pointDiameter = 6
    private val rulerMargin = 30
    private val labelWidth = 320

    private val allLabels: MutableList<ArrivalLabel> = mutableListOf()

    private val timelineNameLabel = JLabel(timelineConfig.label, SwingConstants.CENTER).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        background = Color.WHITE
        foreground = Color.BLACK
        isOpaque = true
    }

    init {
        isOpaque = false

        timelineState.addListener {
            if (it.propertyName == "sequence") {
                updateLabels()
                repaint()
            }
        }

        add(timelineNameLabel)
    }

    override fun doLayout() {
        super.doLayout()

        updateLabels()

        val ruler = timelineView.getRulerBounds()

        val leftSideLabels = allLabels.filter { it.arrival.finalFix == timelineConfig.targetFixes[0] }
        val rightSideLabels = allLabels.filter { it.arrival.finalFix == timelineConfig.targetFixes[1] }

        rearrangeLabel(leftSideLabels, ruler.x - labelWidth - rulerMargin)
        rearrangeLabel(rightSideLabels, ruler.x + rulerMargin + ruler.width)

        timelineNameLabel.setBounds(ruler.x - 10, ruler.y + ruler.height - 20, 100, 20)
    }

    private fun rearrangeLabel(selectedLabels: List<ArrivalLabel>, x: Int) {
        var previousTop: Int? = null
        selectedLabels.sortedBy { it.arrival.finalFixEta }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.arrival.finalFixEta) - 10
            val labelY =
                if (previousTop == null)
                    dotY
                else
                    min(previousTop!! - 3, dotY)

            label.setBounds(x, labelY, labelWidth, 20)
            previousTop = label.y - label.height
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        doLayout()
        val ruler = timelineView.getRulerBounds()

        allLabels.forEach {
            val leftSide = timelineConfig.targetFixes[0] == it.arrival.finalFix

            val dotX = if (leftSide) ruler.x else ruler.x + ruler.width
            val dotY = timelineView.calculateYPositionForInstant(it.arrival.finalFixEta)
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

    private fun updateLabels() {
        timelineState.arrivals.forEach { arrival ->
            val label = allLabels.find { it.arrival.callSign == arrival.callSign }
            if (label != null) {
                label.arrival = arrival
                label.updateText(timelineState)
            } else {
                val newLabel = ArrivalLabel(arrival)
                // set monospace font:
                newLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                newLabel.foreground = Color.WHITE

                allLabels.add(newLabel)
                add(newLabel)
            }
        }
    }



    private fun paintHourglass(g: Graphics, xPosition: Int) {
        val nowY = timelineView.calculateYPositionForInstant(timelineState.timeNow)
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

