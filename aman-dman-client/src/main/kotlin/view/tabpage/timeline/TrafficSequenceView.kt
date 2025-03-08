package org.example.presentation.tabpage.timeline

import org.example.model.RunwayDelayOccurrence
import org.example.model.TimelineState
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel

class TrafficSequenceView(
    val timelineView: TimelineView,
    val timelineState: TimelineState,
    val alignment: TimelineAlignment
) : JPanel() {

    private val lineLength = 30
    private val lineToLabel = 10

    init {
        background = Color.decode("#323232")
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val delays = timelineState.timelineOccurrences.filterIsInstance<RunwayDelayOccurrence>()

        delays.forEach {
            val y = timelineView.calculateYPositionForInstant(it.time)
            it.delay.toComponents { hours, minutes, seconds, nanoseconds ->
                "$hours:$minutes:$seconds"
            }

            val text = it.name.uppercase() + "  " + it.delay

            when (alignment) {
                TimelineAlignment.LEFT -> {
                    g.color = Color.RED
                    g.drawLine(0, y, lineLength, y)
                    g.color = Color.WHITE
                    g.drawString(text, lineLength + lineToLabel, y)
                }
                TimelineAlignment.RIGHT -> {
                    g.color = Color.RED
                    g.drawLine(width, y, width - lineLength, y)
                    g.color = Color.WHITE
                    val textWidth = g.fontMetrics.stringWidth(text)
                    g.drawString(text, width - textWidth - lineLength - lineToLabel, y)
                }
            }
        }
    }
}