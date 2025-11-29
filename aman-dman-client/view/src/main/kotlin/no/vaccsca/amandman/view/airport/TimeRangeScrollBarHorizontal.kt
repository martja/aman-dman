package no.vaccsca.amandman.view.airport

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.entity.SharedValue
import java.awt.*
import java.awt.event.MouseEvent
import kotlin.math.roundToInt
import kotlin.time.Duration

class TimeRangeScrollBarHorizontal(
    selectedRange: SharedValue<TimeRange>,
    availableRange: SharedValue<TimeRange>
) : TimeRangeScrollBarAbstract(selectedRange, availableRange, inverted = false) {

    override fun getInitialSize(): Dimension {
        return Dimension(0, scrollbarWidth)
    }

    override fun getBarStart(): Int {
        val totalRangeSeconds = availableTimelineSeconds()
        val startOffset = selectedRange.value.start.epochSeconds - availableRange.value.start.epochSeconds
        val relativeStart = (startOffset.toFloat() / totalRangeSeconds)
        return (relativeStart * width).toInt()
    }

    override fun getBarEnd(): Int {
        val totalRangeSeconds = availableTimelineSeconds()
        val endOffset = selectedRange.value.end.epochSeconds - availableRange.value.start.epochSeconds
        val relativeEnd = (endOffset.toFloat() / totalRangeSeconds)
        return (relativeEnd * width).toInt()
    }

    override fun axisPosition(e: MouseEvent): Int = e.x

    override fun getScrollbarLength(): Int = width

    override fun drawScrollBar(g2: Graphics2D) {
        val barStart = getBarStart()
        val barEnd = getBarEnd()

        g2.color = background
        g2.fillRect(barEnd, 0, barStart - barEnd, height)
        g2.color = foreground
        g2.drawRect(barEnd, 0, barStart - barEnd - 1, height - 1)

        timelineEvents.forEach { occurrence ->
            drawEvent(g2, occurrence.scheduledTime, foreground)
        }

        val handleTop = scrollHandleMargin

        val gradient = GradientPaint(
            barStart.toFloat(), handleTop.toFloat(), Color(255, 255, 255, 40),
            barStart.toFloat(), (handleTop + scrollHandleWidth).toFloat(), Color(255, 255, 255, 20)
        )
        g2.paint = gradient
        g2.fillRoundRect(barStart, handleTop, barEnd - barStart, scrollHandleWidth, cornerRadius, cornerRadius)
        g2.color = foreground
        g2.drawRoundRect(barStart, handleTop, barEnd - barStart, scrollHandleWidth, cornerRadius, cornerRadius)

        // Draw resize handles
        g2.fillRoundRect(barStart, handleTop, resizeHandleThickness, scrollHandleWidth, cornerRadius, cornerRadius)
        g2.fillRoundRect(barEnd - resizeHandleThickness, handleTop, resizeHandleThickness, scrollHandleWidth, cornerRadius, cornerRadius)
    }

    override fun drawEvent(g: Graphics, instant: Instant, color: Color) {
        val etaOffset = instant.epochSeconds - availableRange.value.start.epochSeconds
        val relative = (etaOffset.toFloat() / availableTimelineSeconds())
        val xPos = (relative * width).toInt()
        g.color = color
        g.drawLine(xPos, scrollHandleMargin * 2, xPos, height - scrollHandleMargin * 2 - 1)
    }

    override fun drawHighlight(g: Graphics, instant: Instant, duration: Duration, color: Color) {
        val totalRangeSeconds = availableTimelineSeconds().toFloat()
        val startX = ((instant.epochSeconds - availableRange.value.start.epochSeconds) / totalRangeSeconds) * width
        val endX = ((instant.plus(duration).epochSeconds - availableRange.value.start.epochSeconds) / totalRangeSeconds) * width
        g.color = color
        g.fillRect(endX.roundToInt(), 0, (startX - endX).roundToInt(), height)
    }

    override fun drawNowIndicator(g: Graphics2D) {
        val relNow = (NtpClock.now().epochSeconds - availableRange.value.start.epochSeconds).toFloat() / availableTimelineSeconds()
        val pos = (relNow * getScrollbarLength()).toInt()
        g.color = foreground
        drawAxisLine(g, pos)
    }

    override fun drawAxisLine(g: Graphics2D, pos: Int) {
        g.drawLine(pos, 0, pos, height)
    }
}
