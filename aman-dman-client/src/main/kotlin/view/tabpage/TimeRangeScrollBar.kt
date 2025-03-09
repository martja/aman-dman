import kotlinx.datetime.Instant
import org.example.controller.TabController
import org.example.model.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TimeRangeScrollBar(
    private val tabController: TabController,
    private val tabState: TabState
) : JComponent() {
    private var dragging = false
    private var resizingTop = false
    private var resizingBottom = false
    private var lastMouseY = 0

    private var relativeTimeStart = 0.0f
    private var relativeTimeEnd = 0.0f
    private var totalRangeSeconds = 0L
    private var availableTimelineSeconds = 0L
    private var nowRelativeTime = 0.0f

    init {
        preferredSize = Dimension(28, 0) // Fixed width, vertical height

        // Update the scrollbar values when the state changes
        tabState.addListener { evt ->
            totalRangeSeconds = tabState.selectedViewMax.epochSeconds - tabState.timelineMinTime.epochSeconds
            availableTimelineSeconds = tabState.timelineMaxTime.epochSeconds - tabState.timelineMinTime.epochSeconds

            val startPositionOffset = tabState.selectedViewMin.epochSeconds - tabState.timelineMinTime.epochSeconds
            val endPositionOffset = tabState.selectedViewMax.epochSeconds - tabState.timelineMinTime.epochSeconds
            val nowTimePositionOffset = tabState.timeNow.epochSeconds - tabState.timelineMinTime.epochSeconds

            relativeTimeEnd = 1 - endPositionOffset.toFloat() / availableTimelineSeconds.toFloat()
            relativeTimeStart = 1 - startPositionOffset.toFloat() / availableTimelineSeconds.toFloat()
            nowRelativeTime = 1 - nowTimePositionOffset.toFloat() / availableTimelineSeconds.toFloat()

            repaint()
        }

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val barTop = height * relativeTimeEnd
                val barBottom = height * relativeTimeStart

                when {
                    e.y.toFloat() in barTop - 5..barTop + 5 -> {
                        resizingTop = true
                    }
                    e.y.toFloat() in barBottom - 5..barBottom + 5 -> {
                        resizingBottom = true
                    }
                    e.y.toFloat() in barTop..barBottom -> {
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

                val secondsPerPixel = availableTimelineSeconds / height.toFloat()
                val delta: Duration = (- deltaY * secondsPerPixel).toInt().seconds

                when {
                    dragging -> {
                        tabController.moveTimeRange(delta)
                    }
                    resizingTop -> {
                        tabController.moveTimeRangeEnd(delta)
                    }
                    resizingBottom -> {
                        tabController.moveTimeRangeStart(delta)
                    }
                }
            }
        })
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

        val barTop = (relativeTimeEnd * height).toInt()
        val barBottom = (relativeTimeStart * height).toInt()
        val currentTimeY = (nowRelativeTime * height).toInt()

        g2.color = Color.WHITE

        // Draw a line where the current time is
        g2.drawLine(0, currentTimeY, width, currentTimeY)

        val scrollHandleWidth = width - scrollHandleMargin * 2 - 1

        // Draw one line per item
        tabController.getAllTimelineOccurrences().forEach { item ->
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
        val etaOffset = instant.epochSeconds - tabState.timelineMinTime.epochSeconds
        val yPos = (1 - etaOffset.toFloat() /  availableTimelineSeconds.toFloat())*height

        g.color = color
        g.drawLine(scrollHandleMargin * 2, yPos.toInt(), width - scrollHandleMargin * 2 - 1, yPos.toInt())
    }

    private fun drawVerticalBar(g: Graphics, instant: Instant, duration: Duration, color: Color) {
        val etaStartOffset = instant.epochSeconds - tabState.timelineMinTime.epochSeconds
        val etaEndOffset = (instant + duration).epochSeconds - tabState.timelineMinTime.epochSeconds
        val yStartPos = (1 - etaStartOffset.toFloat() / availableTimelineSeconds.toFloat()) * height
        val yEndPos = (1 - etaEndOffset.toFloat() / availableTimelineSeconds.toFloat()) * height

        g.color = Color.RED
        g.fillRect(1, yEndPos.roundToInt(), 3, (yStartPos - yEndPos).roundToInt())
    }
}
