package no.vaccsca.amandman.view.tabpage.timeline

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.common.*
import no.vaccsca.amandman.model.domain.valueobjects.Flight
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.FixInboundEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayDelayEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.view.tabpage.timeline.labels.ArrivalLabel
import no.vaccsca.amandman.view.tabpage.timeline.labels.DepartureLabel
import no.vaccsca.amandman.view.tabpage.timeline.labels.TimelineLabel
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Polygon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.text.SimpleDateFormat
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import kotlin.math.min

class TimelineOverlay(
    val timelineConfig: TimelineConfig,
    val timelineView: TimelineView,
    val presenterInterface: PresenterInterface
) : JPanel(null) {
    private val pointDiameter = 6
    private val scaleMargin = 30
    private val labelWidth = 240
    private val labelHeight = 20

    private val labels: HashMap<String, TimelineLabel> = hashMapOf()

    private var leftEvents: List<TimelineEvent>? = null
    private var rightEvents: List<TimelineEvent>? = null

    private var proposedTime: Instant? = null
    private var proposedTimeIsAvailable: Boolean = false

    private val timeFormat = SimpleDateFormat("HH:mm")

    private var isDraggingLabel: Boolean = false
    private var draggedLabelCopy: TimelineLabel? = null
    private var draggedLabelOriginalX: Int = 0

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
        timelineNameLabel.setBounds(scale.x - 10, scale.y + scale.height - labelHeight, 100, labelHeight)
    }

    fun updateDraggedLabel(timelineEvent: TimelineEvent, proposedTime: Instant, available: Boolean) {
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
            val centerY = dotY - labelHeight / 2

            val labelX = timelineView.getScaleBounds().x - labelWidth - scaleMargin
            val labelY =
                if (previousTopLeft == null)
                    centerY
                else
                    min(previousTopLeft!! - 3, centerY)

            label.setBounds(labelX, labelY, labelWidth, labelHeight)
            previousTopLeft = label.y - labelHeight
        }

        rightLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - labelHeight / 2

            val labelX = timelineView.getScaleBounds().x + timelineView.getScaleBounds().width + scaleMargin
            val labelY =
                if (previousTopRight == null)
                    centerY
                else
                    min(previousTopRight!! - 3, centerY)

            label.setBounds(labelX, labelY, labelWidth, labelHeight)
            previousTopRight = label.y - labelHeight
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        doLayout()

        drawLineFromLabelsToTimeScale(g)

        // Draw line from dragged label copy to timeline ruler
        draggedLabelCopy?.let { copy ->
            val scaleBounds = timelineView.getScaleBounds()
            val isOnRightSide = copy.x > scaleBounds.x
            val labelX = if (isOnRightSide) copy.x else copy.x + copy.width
            val dotX = if (isOnRightSide) scaleBounds.x + scaleBounds.width else scaleBounds.x
            val labelCenterY = copy.y + copy.height / 2

            proposedTime?.let { proposedTime ->
                if (proposedTimeIsAvailable) {
                    g.color = Color.WHITE
                    paintHourglass(g, dotX, proposedTime)
                    g.drawLine(labelX, labelCenterY, dotX, labelCenterY)
                }
            }
        }

        val scaleBounds = timelineView.getScaleBounds()
        val now = Clock.System.now()
        paintHourglass(g, scaleBounds.x, now)
        paintHourglass(g, scaleBounds.x + scaleBounds.width, now)

        // Paint UTC HH:MM at the proposed time if available
        if (proposedTime != null && proposedTimeIsAvailable) {
            val proposedY = timelineView.calculateYPositionForInstant(proposedTime!!)
            g.color = Color.YELLOW
            g.drawString(
                timeFormat.format(proposedTime!!.toEpochMilliseconds()),
                scaleBounds.x + 10,
                proposedY + 5
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

            g.color = if ((label.timelineEvent as RunwayArrivalEvent).sequenceStatus == SequenceStatus.OK) {
                Color.WHITE
            } else {
                Color.GRAY
            }

            g.drawLine(labelX, label.y + labelHeight / 2, dotX, dotY)
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
            presenterInterface.onAircraftSelected(flight.callsign)
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
        val newLabel = when (this) {
            //is FixInboundEvent -> ArrivalLabel(this)
            is DepartureEvent -> DepartureLabel(this)
            is RunwayArrivalEvent -> ArrivalLabel(this, presenterInterface)
            else -> throw IllegalArgumentException("Unsupported occurrence type")
        }

        newLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        newLabel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                // Store original X position
                draggedLabelOriginalX = newLabel.x

                // Create a copy when drag starts
                val copy = when (newLabel.timelineEvent) {
                    is DepartureEvent -> DepartureLabel(newLabel.timelineEvent as DepartureEvent)
                    is RunwayArrivalEvent -> ArrivalLabel(newLabel.timelineEvent as RunwayArrivalEvent, presenterInterface)
                    else -> return
                }
                copy.font = newLabel.font
                copy.bounds = newLabel.bounds
                copy.isOpaque = true
                copy.background = newLabel.background.darker()
                copy.updateText()
                draggedLabelCopy = copy
                add(copy)
                setComponentZOrder(copy, 0) // Bring to front

                newLabel.onDragStart()
            }

            override fun mouseReleased(e: MouseEvent) {
                // Remove the copy when drag ends
                draggedLabelCopy?.let {
                    remove(it)
                    draggedLabelCopy = null
                    repaint()
                }

                if (!isDraggingLabel) {
                    handleLabelClick(newLabel)
                    return
                }
                isDraggingLabel = false
                proposedTime = null // Reset proposed time after dragging
                val pointInView = SwingUtilities.convertPoint(e.component, e.point, timelineView)
                val newInstant = timelineView.calculateInstantForYPosition(pointInView.y)
                onLabelDropped(newLabel.timelineEvent, newInstant)
                newLabel.onDragEnd()
            }
        })
        newLabel.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                isDraggingLabel = true

                draggedLabelCopy?.let { copy ->
                    val pointInOverlay = SwingUtilities.convertPoint(e.component, e.point, this@TimelineOverlay)
                    copy.setLocation(copy.x, pointInOverlay.y - copy.height / 2)
                    repaint() // Trigger repaint to update the line
                }

                val pointInView = SwingUtilities.convertPoint(e.component, e.point, timelineView)
                val newInstant = timelineView.calculateInstantForYPosition(pointInView.y)
                presenterInterface.onLabelDrag(timelineConfig.airportIcao, newLabel.timelineEvent, newInstant)
            }
        })

        return newLabel
    }

    private fun onLabelDropped(timelineEvent: TimelineEvent, newTime: Instant) {
        if (timelineEvent is RunwayArrivalEvent) {
            presenterInterface.beginRunwaySelection(timelineEvent) { selectedRunway ->
                presenterInterface.onLabelDragEnd(timelineConfig.airportIcao, timelineEvent, newTime,selectedRunway)
            }
        }
    }

    private fun paintHourglass(g: Graphics, xPosition: Int, atInstant: Instant) {
        val nowY = timelineView.calculateYPositionForInstant(atInstant)
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
