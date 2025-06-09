package tabpage.timeline

import org.example.dto.TimelineData
import kotlinx.datetime.Clock
import org.example.*
import ControllerInterface
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
    val controllerInterface: ControllerInterface
) : JPanel(null) {
    private val pointDiameter = 6
    private val scaleMargin = 30
    private val labelWidth = 210

    private val labels: HashMap<String, TimelineLabel> = hashMapOf()

    private var leftOccurrences: List<TimelineOccurrence>? = null
    private var rightOccurrences: List<TimelineOccurrence>? = null

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

    fun updateTimelineData(timelineData: TimelineData) {
        this.leftOccurrences = timelineData.left
        this.rightOccurrences = timelineData.right
        val allOccurrences = (leftOccurrences ?: emptyList()) + (rightOccurrences ?: emptyList())
        syncLabelsWithOccurrences(labels, allOccurrences)
        repaint()
    }

    override fun doLayout() {
        super.doLayout()
        rearrangeLabels()
        val scale = timelineView.getScaleBounds()
        timelineNameLabel.setBounds(scale.x - 10, scale.y + scale.height - 20, 100, 20)
    }

    private fun rearrangeLabels() {
        var previousTopLeft: Int? = null
        var previousTopRight: Int? = null

        val leftLabels = labels.values.filter { leftOccurrences?.contains(it.timelineOccurrence) ?: false }
        val rightLabels = labels.values.filter { rightOccurrences?.contains(it.timelineOccurrence) ?: false }

        leftLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - label.height / 2

            val labelX = timelineView.getScaleBounds().x - labelWidth - scaleMargin
            val labelY =
                if (previousTopLeft == null)
                    centerY
                else
                    min(previousTopLeft!! - 3, centerY)

            label.setBounds(labelX, labelY, labelWidth, 20)
            previousTopLeft = label.y - label.height
        }

        rightLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - label.height / 2

            val labelX = timelineView.getScaleBounds().x + timelineView.getScaleBounds().width + scaleMargin
            val labelY =
                if (previousTopRight == null)
                    centerY
                else
                    min(previousTopRight!! - 3, centerY)

            label.setBounds(labelX, labelY, labelWidth, 20)
            previousTopRight = label.y - label.height
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        doLayout()

        drawLineFromLabelsToTimeScale(g)

        val scaleBounds = timelineView.getScaleBounds()
        paintHourglass(g, scaleBounds.x)
        paintHourglass(g, scaleBounds.x + scaleBounds.width)
    }

    private fun drawLineFromLabelsToTimeScale(g: Graphics) {
        val scaleBounds = timelineView.getScaleBounds()
        labels.values.forEach { label ->
            val isOnRightSide = label.x > scaleBounds.x

            val labelX = if (isOnRightSide) label.x else label.x + label.width
            val dotX = if (isOnRightSide) scaleBounds.x + scaleBounds.width  else scaleBounds.x
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            g.drawLine(labelX, label.y + label.height / 2, dotX, dotY)
            g.fillOval(
                dotX - pointDiameter / 2,
                dotY - pointDiameter / 2,
                pointDiameter,
                pointDiameter,
            )
        }
    }

    private fun syncLabelsWithOccurrences(currentLabels: HashMap<String, TimelineLabel>, occurrences: List<TimelineOccurrence>?) {
        removeOld(
            fromLabels = currentLabels,
            currentCallsigns = occurrences?.mapNotNull { it.getFlight()?.callsign } ?: emptyList()
        )
        occurrences?.forEach { timelineOccurrence ->
            val flight = timelineOccurrence.getFlight()
            if (flight != null) {
                val label = currentLabels[flight.callsign]
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
                    currentLabels[flight.callsign] = newLabel
                    add(newLabel)
                }
            }
        }
    }

    private fun removeOld(fromLabels: HashMap<String, TimelineLabel>, currentCallsigns: List<String>) {
        val iterator = fromLabels.entries.iterator()
        while (iterator.hasNext()) {
            val (callsign, label) = iterator.next()
            if (callsign !in currentCallsigns) {
                iterator.remove()
                remove(label)
            }
        }
    }

    private fun handleLabelClick(label: TimelineLabel) {
        val flight = label.timelineOccurrence.getFlight()
        if (flight != null) {
            controllerInterface.onAircraftSelected(flight.callsign)
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

