package org.example.presentation.tabpage.timeline

import org.example.state.DelayDefinition
import org.example.state.TimelineState
import java.awt.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JPanel


class Ruler(val timelineView: ITimelineView, val state: TimelineState) : JPanel(null) {
    private val TICK_WIDTH_1_MIN = 5
    private val TICK_WIDTH_5_MIN = 10

    private val lineColor = Color.decode("#C8C8C8")
    private val pastColor = Color.decode("#4B4B4B")

    init {
        background = Color.decode("#646464")

    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val timeNow = Instant.now()
        val timespanSeconds = state.selectedViewMax.epochSecond - state.selectedViewMin.epochSecond

        // Set background color of time that has passed
        val currentTimeYpos = timelineView.calculateYPositionForInstant(timeNow)
        g.color = pastColor
        g.fillRect(0, currentTimeYpos, width, height - currentTimeYpos)

        // Draw left and right border
        g.color = lineColor
        g.drawLine(0, 0, 0, height)
        g.drawLine(width-1, 0, width-1, height)

        for (timestep in 0 .. timespanSeconds) {
            val accInstant = Instant.ofEpochSecond(state.selectedViewMin.epochSecond + timestep)
            val accSeconds = accInstant.epochSecond
            val yPos = timelineView.calculateYPositionForInstant(Instant.ofEpochSecond(accSeconds))

            if (accSeconds % (60L * 5L) == 0L) {
                g.drawLine(0, yPos, TICK_WIDTH_5_MIN, yPos)
                g.drawLine(width, yPos, width - TICK_WIDTH_5_MIN, yPos)

                if (accSeconds % (60L * 10L) == 0L) {
                    g.drawCenteredString(accInstant.format("HH:mm"), Rectangle(0, yPos - g.fontMetrics.height / 2, width, g.fontMetrics.height), g.font)
                } else {
                    g.drawCenteredString(accInstant.format("mm"), Rectangle(0, yPos - g.fontMetrics.height / 2, width, g.fontMetrics.height), g.font)
                }
            } else if (accSeconds % 60L == 0L) {
                g.drawLine(0, yPos, TICK_WIDTH_1_MIN, yPos)
                g.drawLine(width, yPos, width - TICK_WIDTH_1_MIN, yPos)
            }
        }
        drawDelays(g, state.delays)
    }

    private fun drawDelays(g: Graphics, delays: List<DelayDefinition>) {
        delays.forEach {
            val topY = timelineView.calculateYPositionForInstant(it.to)
            val height = timelineView.calculateYPositionForInstant(it.from) - topY
            g.color = Color.RED
            g.fillRect(0, topY, 2, height)
        }
    }

    private fun Instant.format(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return this.atZone(ZoneId.of("UTC")).format(formatter)
    }

    private fun Graphics.drawCenteredString(text: String, rect: Rectangle, font: Font) {
        val metrics = this.getFontMetrics(font)
        val x = rect.x + (rect.width - metrics.stringWidth(text)) / 2
        val y = rect.y + ((rect.height - metrics.height) / 2) + metrics.ascent
        this.font = font
        this.drawString(text, x, y)
    }
}