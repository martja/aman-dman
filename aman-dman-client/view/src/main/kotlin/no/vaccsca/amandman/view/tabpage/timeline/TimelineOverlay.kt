package no.vaccsca.amandman.view.tabpage.timeline

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.common.*
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.*
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.tabpage.timeline.labels.ArrivalLabel
import no.vaccsca.amandman.view.tabpage.timeline.labels.DepartureLabel
import no.vaccsca.amandman.view.tabpage.timeline.labels.TimelineLabel
import no.vaccsca.amandman.view.tabpage.timeline.utils.GraphicUtils.drawStringAdvanced
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.text.SimpleDateFormat
import javax.swing.JPanel
import javax.swing.SwingUtilities

class TimelineOverlay(
    val timelineConfig: TimelineConfig,
    val timelineView: TimelineView,
    val presenterInterface: PresenterInterface,
    val arrivalLabelLayout: List<LabelItem>,
    val departureLabelLayout: List<LabelItem>,
) : JPanel(null) {
    private val baseFont = Font(Font.MONOSPACED, Font.PLAIN, 12)

    // --- Constants ---
    private val labelHBorder = 3         // Horizontal padding inside labels
    private val labelVBorder = 0         // Vertical padding inside labels
    private val pointDiameter = 6       // Diameter of the dot on the timescale
    private val scaleMargin = 30        // Distance between timescale and labels
    private val timelinePadding = 10   // Padding between timeline edge and labels
    private val labelHeight = 15         // Fixed height for labels
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
    init {
        isOpaque = false
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
    }

    fun updateDraggedLabel(timelineEvent: TimelineEvent, proposedTime: Instant, available: Boolean) {
        this.proposedTime = proposedTime
        this.proposedTimeIsAvailable = available
        repaint()
    }

    private fun computedLabelWidth(): Int {
        val maxLabelLength = Math.max(
            arrivalLabelLayout.sumOf { it.width },
            departureLabelLayout.sumOf { it.width }
        )
        val dummyLabelContent = "-".repeat(maxLabelLength)
        val fm = getFontMetrics(baseFont)
        val typicalSize = fm.stringWidth(dummyLabelContent)
        return typicalSize + labelHBorder * 2
    }

    override fun getPreferredSize(): Dimension {
        val dual = timelineConfig.runwaysLeft.isNotEmpty() && timelineConfig.runwaysRight.isNotEmpty()
        val scaleW = timelineView.getScaleWidth()
        val labelWidth = computedLabelWidth()
        val width = if (dual) {
            scaleW + 2 * (labelWidth + scaleMargin) + timelinePadding * 2
        } else {
            scaleW + labelWidth + scaleMargin + timelinePadding
        }

        // Height: keep default (could be derived from parent)
        return Dimension(width, super.getPreferredSize().height)
    }

    // --- Layout ---
    private fun rearrangeLabels() {
        var previousTopLeft: Int? = null
        var previousTopRight: Int? = null
        val labelWidth = computedLabelWidth()

        val leftLabels = labels.values.filter { leftEvents?.contains(it.timelineEvent) == true }
        val rightLabels = labels.values.filter { rightEvents?.contains(it.timelineEvent) == true }

        leftLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - labelHeight / 2
            val labelX = timelineView.getScaleBounds().x - labelWidth - scaleMargin
            val labelY = previousTopLeft?.let { kotlin.math.min(it - 3, centerY) } ?: centerY
            label.setBounds(labelX, labelY, labelWidth, labelHeight)
            previousTopLeft = label.y - labelHeight
        }

        rightLabels.sortedBy { it.getTimelinePlacement() }.forEach { label ->
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val centerY = dotY - labelHeight / 2
            val labelX = timelineView.getScaleBounds().x + timelineView.getScaleBounds().width + scaleMargin
            val labelY = previousTopRight?.let { kotlin.math.min(it - 3, centerY) } ?: centerY
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
        drawTimelineTitle(g)
        drawProposedTime(g)
    }

    private fun drawTimelineTitle(g: Graphics) {
        val scaleBounds = timelineView.getScaleBounds()
        g.color = Color.BLACK

        val isDualTimeline = timelineConfig.runwaysLeft.isNotEmpty() && timelineConfig.runwaysRight.isNotEmpty()
        val vPadding = 2
        val hPadding = 4
        g.drawStringAdvanced(
            text = timelineConfig.title,
            x = if (isDualTimeline) scaleBounds.x + scaleBounds.width / 2 else scaleBounds.x,
            y = scaleBounds.y + scaleBounds.height - g.fontMetrics.height - vPadding * 2,
            backgroundColor = Color.LIGHT_GRAY,
            hPadding = hPadding,
            vPadding = vPadding,
            hCenter = isDualTimeline,
            vCenter = false,
        )
    }

    private fun drawLinesFromLabelsToTimeScale(g: Graphics) {
        val scaleBounds = timelineView.getScaleBounds()
        labels.values.forEach { label ->
            val isOnRightSide = label.x > scaleBounds.x
            val labelX = if (isOnRightSide) label.x else label.x + label.width
            val dotX = if (isOnRightSide) scaleBounds.x + scaleBounds.width else scaleBounds.x
            val dotY = timelineView.calculateYPositionForInstant(label.getTimelinePlacement())
            val event = label.timelineEvent
            g.color = if (event is RunwayArrivalEvent && event.sequenceStatus == SequenceStatus.OK) Color.WHITE else Color.GRAY
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
            val text = timeFormat.format(proposedTime!!.toEpochMilliseconds())
            g.color = Color.YELLOW
            g.drawStringAdvanced(
                text = text,
                x = scaleBounds.x + scaleBounds.width / 2,
                y = proposedY,
                backgroundColor = Color(80, 80, 80),
                hCenter = true,
                vCenter = true,
            )
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
    private fun TimelineEvent.getFlight(): RunwayFlightEvent? = when (this) {
        is DepartureEvent -> this
        is RunwayArrivalEvent -> this
        is RunwayDelayEvent -> null
    }

    private fun TimelineEvent.createLabel(): TimelineLabel {
        val label = when (this) {
            is DepartureEvent -> DepartureLabel(departureLabelLayout, this, hBorder = labelHBorder, vBorder = labelVBorder)
            is RunwayArrivalEvent -> ArrivalLabel(arrivalLabelLayout, this, presenterInterface, hBorder = labelHBorder, vBorder = labelVBorder)
            else -> throw IllegalArgumentException("Unsupported occurrence type")
        }
        label.font = baseFont
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

        override fun mouseClicked(e: MouseEvent?) {
            label.onDragEnd()
            cleanupDraggedLabelCopy()
            handleLabelClick(label)
        }

        override fun mouseReleased(e: MouseEvent) {
            cleanupDraggedLabelCopy()
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
            is DepartureEvent -> DepartureLabel(departureLabelLayout, label.timelineEvent as DepartureEvent, hBorder = labelHBorder, vBorder = labelVBorder)
            is RunwayArrivalEvent -> ArrivalLabel(arrivalLabelLayout, label.timelineEvent as RunwayArrivalEvent, presenterInterface, hBorder = labelHBorder, vBorder = labelVBorder)
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

    private fun cleanupDraggedLabelCopy() {
        draggedLabelCopy?.let {
            remove(it)
            draggedLabelCopy = null
            repaint()
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
