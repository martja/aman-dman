package tabpage.timeline

import org.example.RunwayDelayOccurrence
import org.example.TimelineOccurrence
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel

class SequenceStack(
    val timelineView: TimelineView,
    val alignment: TimelineAlignment
) : JPanel() {

    private val lineLength = 30
    private val lineToLabel = 10

    private var timelineOccurrences: List<TimelineOccurrence> = emptyList()

    init {
        background = Color.decode("#323232")
    }

    fun updateTimelineOccurrences(occurrences: List<TimelineOccurrence>) {
        this.timelineOccurrences = occurrences
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val delays = timelineOccurrences.filterIsInstance<RunwayDelayOccurrence>()

        delays.forEach {
            val y = timelineView.calculateYPositionForInstant(it.scheduledTime)
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