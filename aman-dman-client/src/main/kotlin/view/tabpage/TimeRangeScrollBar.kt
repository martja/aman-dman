import org.example.state.ApplicationState
import java.awt.*
import java.awt.event.*
import javax.swing.*

class TimeRangeScrollBar(val state: ApplicationState) : JComponent() {
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
        state.addListener { evt ->
            totalRangeSeconds = state.selectedViewMax.epochSecond - state.timelineMinTime.epochSecond
            availableTimelineSeconds = state.timelineMaxTime.epochSecond - state.timelineMinTime.epochSecond

            val startPositionOffset = state.selectedViewMin.epochSecond - state.timelineMinTime.epochSecond
            val endPositionOffset = state.selectedViewMax.epochSecond - state.timelineMinTime.epochSecond
            val nowTimePositionOffset = state.timeNow.epochSecond - state.timelineMinTime.epochSecond

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
                val minRangeSec = 60L * 10L
                val deltaY = e.y - lastMouseY
                lastMouseY = e.y

                val secondsPerPixel = availableTimelineSeconds / height.toFloat()
                val deltaSeconds = -(deltaY * secondsPerPixel).toLong()

                when {
                    dragging -> {
                        // Move entire range up/down (scrolling)
                        val newMin = state.selectedViewMin.plusSeconds(deltaSeconds)
                        val newMax = state.selectedViewMax.plusSeconds(deltaSeconds)

                        if (newMin.isAfter(state.timelineMinTime) && newMax.isBefore(state.timelineMaxTime)) {
                            state.selectedViewMin = state.selectedViewMin.plusSeconds(deltaSeconds)
                            state.selectedViewMax = state.selectedViewMax.plusSeconds(deltaSeconds)
                        }
                    }
                    resizingTop -> {
                        // Adjust only the start position (stretching)
                        val newMaxTime = state.selectedViewMax.plusSeconds(deltaSeconds)

                        // Ensure the new start time is within bounds
                        if (newMaxTime.isBefore(state.timelineMaxTime) && newMaxTime.isAfter(state.selectedViewMin.plusSeconds(minRangeSec))) {
                            state.selectedViewMax = newMaxTime
                        }
                    }
                    resizingBottom -> {
                        // Adjust only the end position (stretching)
                        val newMinTime = state.selectedViewMin.plusSeconds(deltaSeconds)

                        // Ensure the new end time is within bounds
                        if (newMinTime.isAfter(state.timelineMinTime) && newMinTime.isBefore(state.selectedViewMax.minusSeconds(minRangeSec))) {
                            state.selectedViewMin = newMinTime
                        }
                    }
                }
                repaint()
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

        g2.drawRoundRect(scrollHandleMargin, barTop, scrollHandleWidth, barBottom - barTop, cornerRadius, cornerRadius)
        g2.fillRoundRect(scrollHandleMargin, barTop, scrollHandleWidth, resizeHandleThickness, cornerRadius, cornerRadius)
        g2.fillRoundRect(scrollHandleMargin, barBottom - resizeHandleThickness, scrollHandleWidth, resizeHandleThickness, cornerRadius, cornerRadius)

        // Draw one line per arrival
        state.arrivals.forEach { arrival ->
            val etaOffset = arrival.eta.epochSecond - state.timelineMinTime.epochSecond
            val yPos = (1 - etaOffset.toFloat() /  availableTimelineSeconds.toFloat())*height
            g2.drawLine(scrollHandleMargin * 2, yPos.toInt(), width - scrollHandleMargin * 2 - 1, yPos.toInt())
        }
    }
}
