package tabpage

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.example.DepartureOccurrence
import org.example.RunwayDelayOccurrence
import org.example.TimelineOccurrence
import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeRangeScrollBar(
    private val onRangeChange : (start: Instant, end: Instant) -> Unit,
) : JComponent() {
    private var dragging = false
    private var resizingTop = false
    private var resizingBottom = false
    private var lastMouseY = 0

    private var selectedViewMin: Instant = Clock.System.now() - 10.minutes
    private var selectedViewMax: Instant = Clock.System.now() + 30.minutes

    private var timelineMinTime: Instant = Instant.DISTANT_PAST
    private var timelineMaxTime: Instant = Instant.DISTANT_FUTURE

    private var timelineOccurrences: List<TimelineOccurrence> = emptyList()

    private var barTop: Int = 0
    private var barBottom: Int = 0

    init {
        preferredSize = Dimension(28, 0) // Fixed width, vertical height

        // Update the scrollbar values when the state changes
        /*tabState.addListener { evt ->
            totalRangeSeconds = tabState.selectedViewMax.epochSeconds - tabState.timelineMinTime.epochSeconds
            availableTimelineSeconds = tabState.timelineMaxTime.epochSeconds - tabState.timelineMinTime.epochSeconds

            val startPositionOffset = tabState.selectedViewMin.epochSeconds - tabState.timelineMinTime.epochSeconds
            val endPositionOffset = tabState.selectedViewMax.epochSeconds - tabState.timelineMinTime.epochSeconds
            val nowTimePositionOffset = tabState.timeNow.epochSeconds - tabState.timelineMinTime.epochSeconds

            relativeTimeEnd = 1 - endPositionOffset.toFloat() / availableTimelineSeconds.toFloat()
            relativeTimeStart = 1 - startPositionOffset.toFloat() / availableTimelineSeconds.toFloat()
            nowRelativeTime = 1 - nowTimePositionOffset.toFloat() / availableTimelineSeconds.toFloat()

            repaint()
        }*/

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when {
                    e.y in barTop - 5..barTop + 5 -> {
                        resizingTop = true
                    }
                    e.y in barBottom - 5..barBottom + 5 -> {
                        resizingBottom = true
                    }
                    e.y in barTop..barBottom -> {
                        dragging = true
                    }
                }
                lastMouseY = e.y
            }

            override fun mouseReleased(e: MouseEvent) {
                dragging = false
                resizingTop = false
                resizingBottom = false
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val deltaY = e.y - lastMouseY
                lastMouseY = e.y

                val secondsPerPixel = availableTimelineSeconds() / height.toFloat()
                val delta: Duration = (- deltaY * secondsPerPixel).toInt().seconds

                when {
                    dragging -> {
                        onRangeChange(
                            selectedViewMin + delta,
                            selectedViewMax + delta
                        )
                    }
                    resizingTop -> {
                        onRangeChange(
                            selectedViewMin,
                            selectedViewMax + delta
                        )
                    }
                    resizingBottom -> {
                        onRangeChange(
                            selectedViewMin + delta,
                            selectedViewMax
                        )
                    }
                }
            }
        })
    }

    fun updateRange(start: Instant, end: Instant) {
        selectedViewMin = start
        selectedViewMax = end
        updateScrollbar()
        repaint()
    }

    fun setTimelineOccurrences(occurrences: List<TimelineOccurrence>) {
        this.timelineOccurrences = occurrences
        this.timelineMinTime = occurrences.minOfOrNull { Clock.System.now() - 30.minutes } ?: Instant.DISTANT_PAST
        this.timelineMaxTime = occurrences.maxOfOrNull { it.time + 1.hours } ?: Instant.DISTANT_FUTURE
        updateScrollbar()
        repaint()
    }

    fun updateScrollbar() {
        val totalRangeSeconds = availableTimelineSeconds()

        val startPositionOffset = selectedViewMin.epochSeconds - timelineMinTime.epochSeconds
        val endPositionOffset = selectedViewMax.epochSeconds - timelineMinTime.epochSeconds

        val relativeTimeEnd = 1 - endPositionOffset.toFloat() / totalRangeSeconds.toFloat()
        val relativeTimeStart = 1 - startPositionOffset.toFloat() / totalRangeSeconds.toFloat()

        barTop = (relativeTimeEnd * height).toInt()
        barBottom = (relativeTimeStart * height).toInt()

        repaint()
    }

    val scrollHandleMargin = 5
    val resizeHandleThickness = 4
    val cornerRadius = 10

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // DARK_GRAY
        g2.color = Color.DARK_GRAY
        g2.fillRect(0, 0, width, height)
        g2.color = Color.WHITE
        g2.drawRect(0, 0, width - 1, height - 1)

        val nowRelativeTime = 1 - (Clock.System.now().epochSeconds - timelineMinTime.epochSeconds).toFloat() / (timelineMaxTime.epochSeconds - timelineMinTime.epochSeconds).toFloat()
        val currentTimeY = (nowRelativeTime * height).toInt()

        g2.color = Color.WHITE

        // Draw a line where the current time is
        g2.drawLine(0, currentTimeY, width, currentTimeY)

        val scrollHandleWidth = width - scrollHandleMargin * 2 - 1

        // Draw one line per item
        timelineOccurrences.forEach { item ->
            when (item) {
                is RunwayDelayOccurrence ->
                    drawVerticalBar(g2, item.time, item.delay, Color.RED)
                is DepartureOccurrence ->
                    drawHorizontalBar(g2, item.time, Color.decode("#83989B"))
                else ->
                    drawHorizontalBar(g2, item.time, Color.WHITE)
            }
        }

        g2.color = Color(0, 0, 0, 50)
        g2.fillRoundRect(scrollHandleMargin, barTop, scrollHandleWidth, barBottom - barTop, cornerRadius, cornerRadius)
        g2.color = Color.WHITE
        g2.drawRoundRect(scrollHandleMargin, barTop, scrollHandleWidth, barBottom - barTop, cornerRadius, cornerRadius)
        g2.fillRoundRect(scrollHandleMargin, barTop, scrollHandleWidth, resizeHandleThickness, cornerRadius, cornerRadius)
        g2.fillRoundRect(scrollHandleMargin, barBottom - resizeHandleThickness, scrollHandleWidth, resizeHandleThickness, cornerRadius, cornerRadius)
    }

    private fun drawHorizontalBar(g: Graphics, instant: Instant, color: Color) {
        val etaOffset = instant.epochSeconds - timelineMinTime.epochSeconds
        val yPos = (1 - etaOffset.toFloat() /  availableTimelineSeconds().toFloat())*height

        g.color = color
        g.drawLine(scrollHandleMargin * 2, yPos.toInt(), width - scrollHandleMargin * 2 - 1, yPos.toInt())
    }

    private fun drawVerticalBar(g: Graphics, instant: Instant, duration: Duration, color: Color) {
        val etaStartOffset = instant.epochSeconds - timelineMinTime.epochSeconds
        val etaEndOffset = (instant + duration).epochSeconds - timelineMinTime.epochSeconds
        val yStartPos = (1 - etaStartOffset.toFloat() / availableTimelineSeconds().toFloat()) * height
        val yEndPos = (1 - etaEndOffset.toFloat() / availableTimelineSeconds().toFloat()) * height

        g.color = Color.RED
        g.fillRect(1, yEndPos.roundToInt(), 3, (yStartPos - yEndPos).roundToInt())
    }

    private fun availableTimelineSeconds(): Long {
        return timelineMaxTime.epochSeconds - timelineMinTime.epochSeconds
    }
}
