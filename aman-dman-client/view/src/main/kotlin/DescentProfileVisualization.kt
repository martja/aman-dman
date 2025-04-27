import org.example.TrajectoryPoint
import util.WindBarbs
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.max

class DescentProfileVisualization : JPanel(BorderLayout()) {

    private var trajectoryPoints: List<TrajectoryPoint> = emptyList()
    private var hoveredPoint: TrajectoryPoint? = null

    init {
        background = java.awt.Color.DARK_GRAY

        addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val point = findClosestTrajectoryPointAlongXAxis(e.x)
                if (point != hoveredPoint) {
                    hoveredPoint = point
                    repaint()
                }
            }
        })
    }


    fun setDescentSegments(segments: List<TrajectoryPoint>) {
        this.trajectoryPoints = segments
        repaint()
    }

    private val diagramMargin = 30
    private val diagramMarginTop = 90

    override fun paintComponent(g: java.awt.Graphics) {
        super.paintComponent(g)

        if (trajectoryPoints.isEmpty()) {
            return
        }

        val altitudeRange = IntRange(
            trajectoryPoints.minOf { it.altitude },
            trajectoryPoints.maxOf { it.altitude }
        )

        // Illustrate the flight levels
        for (i in altitudeRange step 1000) {
            val (_, yPos) = calculateComponentCoordinates(i - altitudeRange.min(), altitudeRange, 0.0f)
            g.color = java.awt.Color.GRAY
            g.drawLine(50, yPos, width - diagramMargin, yPos)
            g.drawString("FL" + (i / 100).toString().padStart(3, '0'), 5, yPos + 5)
        }

        var prevX = diagramMargin
        var prevY = diagramMarginTop
        var prevSpeedY: Int? = null

        trajectoryPoints.forEach {
            val (xPosAlt, yPosAlt) = calculateComponentCoordinates(it.altitude, altitudeRange, it.remainingDistance)
            val (xPosGs, yPosGs) = calculateComponentCoordinates(it.groundSpeed, IntRange(0, 600), it.remainingDistance)

            // Visualize speed wrt height
            g.color = java.awt.Color.YELLOW
            prevSpeedY?.let { y -> g.drawLine(prevX, y, xPosGs, yPosGs) }
            prevSpeedY = yPosGs

            // Visualize remaninging distance wrt height
            g.color = java.awt.Color.WHITE
            g.drawLine(prevX, prevY, xPosAlt, yPosAlt)
            prevX = xPosAlt
            prevY = yPosAlt

            it.fixId?.let { fixId ->
                g.drawString(fixId, xPosAlt, yPosAlt - 5)
                g.drawString(it.remainingTime.toString(), xPosAlt, yPosAlt + 15)
                g.drawString(it.groundSpeed.toString() + " kts", xPosAlt, yPosAlt + 15 + 12)
                g.drawString(it.altitude.toString() + " ft", xPosAlt, yPosAlt + 15 + 10 + 12)
            }

            g.drawOval(xPosAlt - 3, yPosAlt - 3, 6, 6)

            WindBarbs.drawWindBarb(g, xPosAlt, 40, it.wind.directionDeg, it.wind.speedKts, relativeToHeading = it.heading + 90 )
        }

        hoveredPoint?.let {
            g.drawString(
                "GS: ${it.groundSpeed} kts, TAS: ${it.tas} kts, IAS: ${it.ias} kts, remaining time: ${it.remainingTime}, remaining distance: ${it.remainingDistance} nm",
                diagramMargin,
                height - 20
            )
            val (xPos, _) = calculateComponentCoordinates(it.altitude, altitudeRange, it.remainingDistance)
            g.color = java.awt.Color.RED
            g.drawLine(xPos, diagramMarginTop, xPos, height - diagramMargin)
        }
    }

    private fun calculateComponentCoordinates(yValue: Int, range: IntRange, distance: Float): Pair<Int, Int> {
        val totalLengthNm = trajectoryPoints.first().remainingDistance

        val pxPerFt = (height - diagramMarginTop*2).toFloat() / (range.max() - range.min()).toFloat()
        val pxPerNm = (width - diagramMargin*2).toFloat() / totalLengthNm

        val yPos = (height - pxPerFt * (yValue - range.min())).toInt() - diagramMarginTop
        val xPos = width - (pxPerNm * distance).toInt() - diagramMargin

        return Pair(xPos, yPos)
    }

    private fun findClosestTrajectoryPointAlongXAxis(x: Int): TrajectoryPoint? {
        val closestPoint = trajectoryPoints.minByOrNull { point ->
            val (xPos, _) = calculateComponentCoordinates(point.altitude, IntRange(0,0), point.remainingDistance)
            abs(x - xPos)
        }
        return closestPoint
    }

}

