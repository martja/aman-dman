package presentation.tabpage.timeline

import org.example.presentation.tabpage.timeline.ITimelineView
import org.example.state.TimelineState
import java.awt.Color
import java.awt.Graphics
import java.awt.Polygon
import java.time.Instant
import javax.swing.JPanel
import kotlin.math.min

class OverlayView(val timelineState: TimelineState, val timelineView: ITimelineView) : JPanel(null) {
    private val pointDiameter = 6
    private val labelRectPadding = 5

    private val labels: List<ArrivalLabel>

    init {
        isOpaque = false
        labels = timelineState.arrivals.map { ArrivalLabel(it) }
        labels.forEach {
            add(it)
        }
    }

    override fun doLayout() {
        super.doLayout()

        var previousTop: Int? = null

        labels.sortedBy { it.arrival.eta }.forEach {
            val dotY = timelineView.calculateYPositionForInstant(it.arrival.eta) - 10
            val labelY =
                if (previousTop == null)
                    dotY
                else
                    min(previousTop!! - 3, dotY)

            it.setBounds(10, labelY, 180, 20)
            previousTop = it.y - it.height
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        doLayout()
        val ruler = timelineView.getRulerBounds()

        labels.forEach {

            val dotY = timelineView.calculateYPositionForInstant(it.arrival.eta)
            g.drawLine(it.x + it.width, it.y + it.height / 2, ruler.x, dotY)
            g.fillOval(
                (ruler.x) - pointDiameter / 2,
                dotY - pointDiameter / 2,
                pointDiameter,
                pointDiameter,
            )
        }

        paintHourglass(g, ruler.x)
        paintHourglass(g, ruler.x + ruler.width)
    }

    private fun paintHourglass(g: Graphics, xPosition: Int) {
        val nowY = timelineView.calculateYPositionForInstant(Instant.now())
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

