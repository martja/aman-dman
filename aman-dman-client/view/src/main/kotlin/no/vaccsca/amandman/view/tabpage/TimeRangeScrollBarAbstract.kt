package no.vaccsca.amandman.view.tabpage

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.dto.TimelineData
import no.vaccsca.amandman.model.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.timelineEvent.RunwayDelayEvent
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.util.SharedValue
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class TimeRangeScrollBarAbstract(
    protected val selectedRange: SharedValue<TimeRange>,
    protected val availableRange: SharedValue<TimeRange>,
    protected val thickness: Int = 28,
    protected val inverted: Boolean = true
) : JComponent() {

    protected var dragging = false
    protected var resizingStart = false
    protected var resizingEnd = false
    protected var lastMouseAxisPos = 0
    protected var timelineEvents: List<TimelineEvent> = emptyList()

    protected val scrollHandleMargin = 5
    protected val scrollHandleThickness = thickness - scrollHandleMargin * 2
    protected val cornerRadius = 10
    protected val resizeHandleThickness = 4

    init {
        preferredSize = this.getInitialSize()

        selectedRange.addListener { repaint() }
        availableRange.addListener { repaint() }

        setupMouseListeners()
    }

    abstract fun getInitialSize(): Dimension
    abstract fun getBarStart(): Int
    abstract fun getBarEnd(): Int
    abstract fun axisPosition(e: MouseEvent): Int
    abstract fun getScrollbarLength(): Int
    abstract fun drawScrollBar(g2: Graphics2D)
    abstract fun drawEvent(g: Graphics, instant: Instant, color: Color)
    abstract fun drawHighlight(g: Graphics, instant: Instant, duration: Duration, color: Color)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = Color.DARK_GRAY
        g2.fillRect(0, 0, width, height)
        g2.color = Color.WHITE
        g2.drawRect(0, 0, width - 1, height - 1)

        drawNowIndicator(g2)
        timelineEvents.toSet().forEach { item ->
            when (item) {
                is RunwayDelayEvent -> drawHighlight(g2, item.scheduledTime, item.delay, Color.RED)
                is DepartureEvent -> drawEvent(g2, item.scheduledTime, Color.decode("#83989B"))
                else -> drawEvent(g2, item.scheduledTime, Color.WHITE)
            }
        }

        drawScrollBar(g2)
    }

    fun updateTimelineEvents(list: List<TimelineData>) {
        this.timelineEvents = list.flatMap { it.left + it.right }
        repaint()
    }

    protected fun availableTimelineSeconds(): Long =
        availableRange.value.end.epochSeconds - availableRange.value.start.epochSeconds

    private fun setupMouseListeners() {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val pos = axisPosition(e)
                val barStart = getBarStart()
                val barEnd = getBarEnd()

                when {
                    pos in (barEnd - 5)..(barEnd + 5) -> resizingEnd = true
                    pos in (barStart - 5)..(barStart + 5) -> resizingStart = true
                    pos in min(barStart, barEnd)..max(barStart, barEnd) -> dragging = true
                }
                lastMouseAxisPos = pos
            }

            override fun mouseReleased(e: MouseEvent) {
                dragging = false
                resizingEnd = false
                resizingStart = false
            }
        })


        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val pos = axisPosition(e)
                val delta =
                    if (inverted)
                        pos - lastMouseAxisPos
                    else
                        lastMouseAxisPos - pos

                lastMouseAxisPos = pos

                val secondsPerPixel = availableTimelineSeconds().toFloat() / getScrollbarLength()
                val durationDelta = (-delta * secondsPerPixel).roundToInt().seconds

                val newRange = when {
                    dragging -> TimeRange(selectedRange.value.start + durationDelta, selectedRange.value.end + durationDelta)
                    resizingStart -> TimeRange(selectedRange.value.start + durationDelta, selectedRange.value.end)
                    resizingEnd -> TimeRange(selectedRange.value.start, selectedRange.value.end + durationDelta)
                    else -> selectedRange.value
                }

                if (
                    newRange.start > availableRange.value.start &&
                    newRange.end < availableRange.value.end &&
                    newRange.end - newRange.start > 10.minutes
                ) {
                    selectedRange.value = newRange
                }
            }
        })
    }

    abstract fun drawNowIndicator(g: Graphics2D)
    abstract fun drawAxisLine(g: Graphics2D, pos: Int)
}
