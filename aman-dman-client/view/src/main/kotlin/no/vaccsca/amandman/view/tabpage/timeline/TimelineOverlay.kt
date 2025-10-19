package no.vaccsca.amandman.view.tabpage.timeline

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.common.*
import no.vaccsca.amandman.model.domain.valueobjects.Flight
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.*
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.view.tabpage.timeline.labels.*
import java.awt.*
import java.awt.event.*
import java.text.SimpleDateFormat
import javax.swing.*
import kotlin.math.min

class TimelineOverlay(
    val timelineConfig: TimelineConfig,
    val timelineView: TimelineView,
    val presenterInterface: PresenterInterface
) : JPanel(null) {

    // --- Constants ---
    private val pointDiameter = 6
    private val scaleMargin = 30
    private val labelWidth = 240
    private val labelHeight = 20
    private val timeFormat = SimpleDateFormat("HH:mm")

    // --- State ---
    private val labels = hashMapOf<String, TimelineLabel>()
    private var leftEvents: List<TimelineEvent>? = null
    private var rightEvents: List<TimelineEvent>? = null
    private var proposedTime: Instant? = null
    private var proposedTimeIsAvailable = false
    private var isDraggingLabel = false
    private var draggedLabelCopy: TimelineLabel? = null
    private var draggedLabelOriginalX = 0

    // --- UI ---
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
        leftEvents = timelineData.left
        rightEvents = timelineData.right
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

    // --- Layout ---
    private fun rearrangeLabels() {
        var previousTopLeft: Int? = null
        var previousTopRight: Int? = null

        val leftLabels = labels.values.filter { leftEvents?.contains(it.timelineEvent) == true }
        val rightLabels = labels.values.filter { rightEvents?.contains(it.timelineEvent) == true }

        leftLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - labelHeight / 2
            val labelX = timelineView.getScaleBounds().x - labelWidth - scaleMargin
            val labelY = previousTopLeft?.let { min(it - 3, centerY) } ?: centerY
            label.setBounds(labelX, labelY, labelWidth, labelHeight)
            previousTopLeft = label.y - labelHeight
        }

        rightLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - labelHeight / 2
            val labelX = timelineView.getScaleBounds().x + timelineView.getScaleBounds().width + scaleMargin
            val labelY = previousTopRight?.let { min(it - 3, centerY) } ?: centerY
            label.setBounds(labelX, labelY, labelWidth, labelHeight)
            previousTopRight = label.y - labelHeight
        }
    }

    // --- Painting ---
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        doLayout()
        drawLinesFromLabelsToTimeScale(g)
        drawDraggedLabelLine(g)
        drawHourglasses(g)
        drawProposedTime(g)
    }

    private fun drawLinesFromLabelsToTimeScale(g: Graphics) {
        val scaleBounds = timelineView.getScaleBounds()
        labels.values.forEach { label ->
            val isOnRightSide = label.x > scaleBounds.x
            val labelX = if (isOnRightSide) label.x else label.x + label.width
            val dotX = if (isOnRightSide) scaleBounds.x + scaleBounds.width else scaleBounds.x
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            g.color = if ((label.timelineEvent as RunwayArrivalEvent).sequenceStatus == SequenceStatus.OK) Color.WHITE else Color.GRAY
            g.drawLine(labelX, label.y + labelHeight / 2, dotX, dotY)
            g.fillOval(dotX - pointDiameter / 2, dotY - pointDiameter / 2, pointDiameter, pointDiameter)
        }
    }

    private fun drawDraggedLabelLine(g: Graphics) {
        draggedLabelCopy?.let { copy ->
            val scaleBounds = timelineView.getScaleBounds()
            val isOnRightSide = copy.x > scaleBounds.x
            val labelX = if (isOnRightSide) copy.x else copy.x + copy.width
            val dotX = if (isOnRightSide) scaleBounds.x + scaleBounds.width else scaleBounds.x
            val labelCenterY = copy.y + copy.height / 2
            proposedTime?.takeIf { proposedTimeIsAvailable }?.let { time ->
                g.color = Color.WHITE
                paintHourglass(g, dotX, time)
                g.drawLine(labelX, labelCenterY, dotX, labelCenterY)
            }
        }
    }

    private fun drawHourglasses(g: Graphics) {
        val scaleBounds = timelineView.getScaleBounds()
        val now = Clock.System.now()
        paintHourglass(g, scaleBounds.x, now)
        paintHourglass(g, scaleBounds.x + scaleBounds.width, now)
    }

    private fun drawProposedTime(g: Graphics) {
        if (proposedTime != null && proposedTimeIsAvailable) {
            val scaleBounds = timelineView.getScaleBounds()
            val proposedY = timelineView.calculateYPositionForInstant(proposedTime!!)
            g.color = Color.YELLOW
            g.drawString(timeFormat.format(proposedTime!!.toEpochMilliseconds()), scaleBounds.x + 10, proposedY + 5)
        }
    }

    // --- Label/Event Sync ---
    private fun syncLabelsWithEvents(currentLabels: HashMap<String, TimelineLabel>, events: List<TimelineEvent>?) {
        removeOldLabels(currentLabels, events?.mapNotNull { it.getFlight()?.callsign } ?: emptyList())
        events?.forEach { event ->
            event.getFlight()?.let { flight ->
                val label = currentLabels[flight.callsign]
                if (label != null) {
                    label.timelineEvent = event
                    label.updateText()
                    label.updateColors()
                } else {
                    val newLabel = event.createLabel()
                    currentLabels[flight.callsign] = newLabel
                    add(newLabel)
                }
            }
        }
    }

    private fun removeOldLabels(currentLabels: HashMap<String, TimelineLabel>, validCallsigns: List<String>) {
        val iterator = currentLabels.entries.iterator()
        while (iterator.hasNext()) {
            val (callsign, label) = iterator.next()
            if (callsign !in validCallsigns) {
                iterator.remove()
                remove(label)
            }
        }
    }

    // --- Label Creation & Dragging ---
    private fun TimelineEvent.getFlight(): Flight? = when (this) {
        is FixInboundEvent -> this
        is DepartureEvent -> this
        is RunwayArrivalEvent -> this
        is RunwayDelayEvent -> null
    }

    private fun TimelineEvent.createLabel(): TimelineLabel {
        val label = when (this) {
            is DepartureEvent -> DepartureLabel(this)
            is RunwayArrivalEvent -> ArrivalLabel(this, presenterInterface)
            else -> throw IllegalArgumentException("Unsupported occurrence type")
        }
        label.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        label.addMouseListener(labelMouseAdapter(label))
        label.addMouseMotionListener(labelMouseMotionAdapter(label))
        return label
    }

    private fun labelMouseAdapter(label: TimelineLabel) = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            draggedLabelOriginalX = label.x
            draggedLabelCopy = createLabelCopy(label)
            draggedLabelCopy?.let {
                add(it)
                setComponentZOrder(it, 0)
            }
            label.onDragStart()
        }

        override fun mouseReleased(e: MouseEvent) {
            draggedLabelCopy?.let {
                remove(it)
                draggedLabelCopy = null
                repaint()
            }
            if (!isDraggingLabel) {
                handleLabelClick(label)
                return
            }
            isDraggingLabel = false
            proposedTime = null
            val pointInView = SwingUtilities.convertPoint(e.component, e.point, timelineView)
            val newInstant = timelineView.calculateInstantForYPosition(pointInView.y)
            onLabelDropped(label.timelineEvent, newInstant)
            label.onDragEnd()
        }
    }

    private fun labelMouseMotionAdapter(label: TimelineLabel) = object : MouseMotionAdapter() {
        override fun mouseDragged(e: MouseEvent) {
            isDraggingLabel = true
            draggedLabelCopy?.let { copy ->
                val pointInOverlay = SwingUtilities.convertPoint(e.component, e.point, this@TimelineOverlay)
                copy.setLocation(copy.x, pointInOverlay.y - copy.height / 2)
                repaint()
            }
            val pointInView = SwingUtilities.convertPoint(e.component, e.point, timelineView)
            val newInstant = timelineView.calculateInstantForYPosition(pointInView.y)
            presenterInterface.onLabelDrag(timelineConfig.airportIcao, label.timelineEvent, newInstant)
        }
    }

    private fun createLabelCopy(label: TimelineLabel): TimelineLabel? {
        val copy = when (label.timelineEvent) {
            is DepartureEvent -> DepartureLabel(label.timelineEvent as DepartureEvent)
            is RunwayArrivalEvent -> ArrivalLabel(label.timelineEvent as RunwayArrivalEvent, presenterInterface)
            else -> return null
        }
        copy.font = label.font
        copy.bounds = label.bounds
        copy.isOpaque = true
        copy.background = label.background.darker()
        copy.updateText()
        return copy
    }

    private fun handleLabelClick(label: TimelineLabel) {
        label.timelineEvent.getFlight()?.let { presenterInterface.onAircraftSelected(it.callsign) }
    }

    private fun onLabelDropped(timelineEvent: TimelineEvent, newTime: Instant) {
        if (timelineEvent is RunwayArrivalEvent) {
            presenterInterface.beginRunwaySelection(timelineEvent) { selectedRunway ->
                presenterInterface.onLabelDragEnd(timelineConfig.airportIcao, timelineEvent, newTime, selectedRunway)
            }
        }
    }

    // --- Drawing Helpers ---
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
