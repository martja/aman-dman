package no.vaccsca.amandman.view.tabpage.timeline

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.controller.ControllerInterface
import no.vaccsca.amandman.common.*
import no.vaccsca.amandman.model.Flight
import no.vaccsca.amandman.model.dto.TimelineData
import no.vaccsca.amandman.model.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.timelineEvent.FixInboundEvent
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.timelineEvent.RunwayDelayEvent
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent
import no.vaccsca.amandman.view.tabpage.timeline.labels.ArrivalLabel
import no.vaccsca.amandman.view.tabpage.timeline.labels.DepartureLabel
import no.vaccsca.amandman.view.tabpage.timeline.labels.TimelineLabel
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Polygon
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import kotlin.math.min

class TimelineOverlay(
    val timelineConfig: TimelineConfig,
    val timelineView: TimelineView,
    val controllerInterface: ControllerInterface
) : JPanel(null) {
    private val pointDiameter = 6
    private val scaleMargin = 30
    private val labelWidth = 240

    private val labels: HashMap<String, TimelineLabel> = hashMapOf()

    private var leftEvents: List<TimelineEvent>? = null
    private var rightEvents: List<TimelineEvent>? = null

    private var proposedTime: Instant? = null
    private var proposedTimeIsAvailable: Boolean = false

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
        this.leftEvents = timelineData.left
        this.rightEvents = timelineData.right
        val allEvents = (leftEvents ?: emptyList()) + (rightEvents ?: emptyList())
        syncLabelsWithEvents(labels, allEvents)
        repaint()
    }

    override fun doLayout() {
        super.doLayout()
        rearrangeLabels()
        val scale = timelineView.getScaleBounds()
        timelineNameLabel.setBounds(scale.x - 10, scale.y + scale.height - 20, 100, 20)
    }

    fun updateDraggedLabel(callsign: String, proposedTime: Instant, available: Boolean) {
        this.proposedTime = proposedTime
        this.proposedTimeIsAvailable = available
        repaint()
    }

    private fun rearrangeLabels() {
        var previousTopLeft: Int? = null
        var previousTopRight: Int? = null

        val leftLabels = labels.values.filter { leftEvents?.contains(it.timelineEvent) ?: false }
        val rightLabels = labels.values.filter { rightEvents?.contains(it.timelineEvent) ?: false }

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

        // Paint a circle at the proposed time if it exists
        proposedTime?.let {
            val dotY = timelineView.calculateYPositionForInstant(it)
            g.color = if (proposedTimeIsAvailable) Color.WHITE else Color.RED
            g.fillOval(
                scaleBounds.x,
                dotY - pointDiameter / 2,
                pointDiameter,
                pointDiameter,
            )
        }
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

    private fun syncLabelsWithEvents(currentLabels: HashMap<String, TimelineLabel>, occurrences: List<TimelineEvent>?) {
        removeOld(
            fromLabels = currentLabels,
            currentCallsigns = occurrences?.mapNotNull { it.getFlight()?.callsign } ?: emptyList()
        )
        occurrences?.forEach { timelineEvent ->
            val flight = timelineEvent.getFlight()
            if (flight != null) {
                val label = currentLabels[flight.callsign]
                if (label != null) {
                    label.timelineEvent = timelineEvent
                    label.updateText()
                    label.updateColors()
                } else {
                    val newLabel = timelineEvent.createLabel()
                    newLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                    newLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent) {
                            handleLabelClick(newLabel)
                        }
                    })
                    newLabel.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
                        override fun mouseDragged(e: MouseEvent) {
                            val pointInView = SwingUtilities.convertPoint(e.component, e.point, timelineView)
                            val newInstant = timelineView.calculateInstantForYPosition(pointInView.y)
                            controllerInterface.onLabelDragged(flight.callsign, newInstant)
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
        val flight = label.timelineEvent.getFlight()
        if (flight != null) {
            controllerInterface.onAircraftSelected(flight.callsign)
        }
    }

    private fun TimelineEvent.getFlight(): Flight? {
        return when (this) {
            is FixInboundEvent -> this
            is DepartureEvent -> this
            is RunwayArrivalEvent -> this
            is RunwayDelayEvent -> null
        }
    }

    private fun TimelineEvent.createLabel(): TimelineLabel {
        return when (this) {
            //is FixInboundEvent -> ArrivalLabel(this)
            is DepartureEvent -> DepartureLabel(this)
            is RunwayArrivalEvent -> ArrivalLabel(this, controllerInterface)
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

