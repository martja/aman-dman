import org.example.EstimatedProfilePoint
import util.WindBarbs
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.ToolTipManager

class DescentProfileVisualization : JPanel(BorderLayout()) {

    private var estimatedProfilePoints: List<EstimatedProfilePoint> = emptyList()
    private var hoveredPoint: EstimatedProfilePoint? = null

    init {
        background = java.awt.Color.DARK_GRAY

        addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val point = findHoveredDataPoint(e.x, e.y)
                if (point != hoveredPoint) {
                    hoveredPoint = point
                    repaint()
                }
            }
        })
    }


    fun setDescentSegments(segments: List<EstimatedProfilePoint>) {
        this.estimatedProfilePoints = segments
        repaint()
    }

    private val diagramMargin = 30
    private val diagramMarginTop = 90

    override fun paintComponent(g: java.awt.Graphics) {
        super.paintComponent(g)

        if (estimatedProfilePoints.isEmpty()) {
            return
        }

        val minAlt = estimatedProfilePoints.minOf { it.altitude }
        val maxAlt = estimatedProfilePoints.maxOf { it.altitude }

        // Illustrate the flight levels
        for (i in 0..maxAlt step 1000) {
            val (_, yPos) = calculateComponentCoordinates(i - minAlt, 0.0f)
            g.color = java.awt.Color.GRAY
            g.drawLine(50, yPos, width - diagramMargin, yPos)
            g.drawString("FL" + (i / 100).toString().padStart(3, '0'), 5, yPos + 5)
        }

        var prevX = diagramMargin
        var prevY = diagramMarginTop
        var prevInbound: String? = null

        estimatedProfilePoints.forEach {
            val (xPos, yPos) = calculateComponentCoordinates(it.altitude, it.remainingDistance)

            // Visualize remaninging time wrt height
            g.color = java.awt.Color.GRAY

            // Visualize remaninging distance wrt height
            g.color = java.awt.Color.WHITE
            g.drawLine(prevX, prevY, xPos, yPos)
            prevX = xPos
            prevY = yPos

            if (prevInbound != it.inbound && prevInbound != null) {
                g.drawString(prevInbound, xPos, yPos - 5)
                g.drawString(it.remainingTime.toString(), xPos, yPos + 15)
                g.drawString(it.groundSpeed.toString() + " kts", xPos, yPos + 15 + 12)
                g.drawString(it.altitude.toString() + " ft", xPos, yPos + 15 + 10 + 12)
            }

            prevInbound = it.inbound

            g.drawOval(xPos - 3, yPos - 3, 6, 6)

            WindBarbs.drawWindBarb(g, xPos, 40, it.wind.directionDeg, it.wind.speedKts, relativeToHeading = it.heading + 90 )
        }

        hoveredPoint?.let {
            g.drawString(
                "GS: ${it.groundSpeed} kts, TAS: ${it.tas} kts, IAS: ${it.ias} kts, remaining time: ${it.remainingTime}, remaining distance: ${it.remainingDistance} nm",
                diagramMargin,
                height - 20
            )
        }
    }

    private fun calculateComponentCoordinates(altitude: Int, distance: Float): Pair<Int, Int> {
        val minAlt = estimatedProfilePoints.minOf { it.altitude }
        val maxAlt = estimatedProfilePoints.maxOf { it.altitude }

        val totalLengthNm = estimatedProfilePoints.first().remainingDistance

        val pxPerFt = (height - diagramMarginTop*2).toFloat() / (maxAlt - minAlt).toFloat()
        val pxPerNm = (width - diagramMargin*2).toFloat() / totalLengthNm

        val yPos = (height - pxPerFt * (altitude - minAlt)).toInt() - diagramMarginTop
        val xPos = width - (pxPerNm * distance).toInt() - diagramMargin

        return Pair(xPos, yPos)
    }

    private fun findHoveredDataPoint(x: Int, y: Int): EstimatedProfilePoint? {
        val minDistance = 10
        for (point in estimatedProfilePoints) {
            val (pointX, pointY) = calculateComponentCoordinates(point.altitude, point.remainingDistance)
            if (Math.abs(pointX - x) < minDistance && Math.abs(pointY - y) < minDistance) {
                return point
            }
        }
        return null
    }

}

