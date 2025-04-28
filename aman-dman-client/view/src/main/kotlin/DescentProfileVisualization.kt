import org.example.TrajectoryPoint
import util.WindBarbs
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.roundToInt

class DescentProfileVisualization : JPanel(BorderLayout()) {

    private var trajectoryPoints: List<TrajectoryPoint> = emptyList()
    private var hoveredPoint: TrajectoryPoint? = null

    private var pxPerFt: Float = 0.0f // Y-axis
    private var pxPerKts: Float = 0.0f // Y-axis
    private var pxPerNm: Float = 0.0f // X-axis

    private val BARB_SPACING = 40

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

    override fun doLayout() {
        super.doLayout()
        if (trajectoryPoints.isNotEmpty()) {
            val altitudeRange = IntRange(0, trajectoryPoints.maxOf { it.altitude })
            val speedRange = IntRange(0, 600)
            pxPerNm = (width - diagramMargin - diagramMargin).toFloat() / trajectoryPoints.first().remainingDistance
            pxPerFt = (height - diagramMargin - diagramMarginTop).toFloat() / (altitudeRange.max() - altitudeRange.min()).toFloat()
            pxPerKts = (height - diagramMargin - diagramMarginTop).toFloat() / (speedRange.max() - speedRange.min()).toFloat()
        }
    }


    fun setDescentSegments(segments: List<TrajectoryPoint>) {
        this.trajectoryPoints = segments
        doLayout()
        repaint()
    }

    private val diagramMargin = 30
    private val diagramMarginTop = 90

    override fun paintComponent(g: java.awt.Graphics) {
        super.paintComponent(g)

        if (trajectoryPoints.isEmpty()) {
            return
        }

        val maxAlt = trajectoryPoints.maxOf { it.altitude }

        // Illustrate the flight levels
        for (i in 0..maxAlt step 1000) {
            val yPos = calculateAltitudeCoordinates(altitude = i)
            g.color = java.awt.Color.GRAY
            g.drawLine(50, yPos, width - diagramMargin, yPos)
            g.drawString("FL" + (i / 100).toString().padStart(3, '0'), 5, yPos + 5)
        }

        var prevX = diagramMargin
        var prevY = diagramMarginTop
        var prevSpeedY: Int? = null

        var distFromPreviousBarb = 0

        trajectoryPoints.forEach {
            val x = calculateXcoordinate(it.remainingDistance)
            val yPosAlt = calculateAltitudeCoordinates(it.altitude)
            val yPosGs = calculateSpeedCoordinates(it.groundSpeed)

            // Visualize speed wrt height
            g.color = java.awt.Color.YELLOW
            prevSpeedY?.let { y -> g.drawLine(prevX, y, x, yPosGs) }
            prevSpeedY = yPosGs

            // Visualize remaninging distance wrt height
            g.color = java.awt.Color.WHITE
            g.drawLine(prevX, prevY, x, yPosAlt)

            it.fixId?.let { fixId ->
                g.drawString(fixId, x, yPosAlt - 5)
                g.drawString(it.remainingTime.toString(), x, yPosAlt + 15)
                g.drawString(it.groundSpeed.toString() + " kts", x, yPosAlt + 15 + 12)
                g.drawString(it.altitude.toString() + " ft", x, yPosAlt + 15 + 10 + 12)
            }

            g.drawOval(x - 3, yPosAlt - 3, 6, 6)

            if (distFromPreviousBarb > BARB_SPACING) {
                distFromPreviousBarb = 0
                WindBarbs.drawWindBarb(g, x, 40, it.wind.directionDeg, it.wind.speedKts, relativeToHeading = it.heading + 90 )
            } else {
                distFromPreviousBarb += (x - prevX)
            }

            prevX = x
            prevY = yPosAlt
        }

        hoveredPoint?.let {
            val x = calculateXcoordinate(it.remainingDistance)

            it.windComponent?.let { windComponent ->
                g.drawString(
                    if (windComponent > 0) "+${it.windComponent} kt" else "${it.windComponent} kt",
                    x + 5,
                    height - diagramMargin - 20
                )
            }

            g.drawString(
                "GS: ${it.groundSpeed} | TAS: ${it.tas} | IAS: ${it.ias} | ETA: ${it.remainingTime} | ${it.remainingDistance.roundToInt()} NM",
                5,
                height - 5
            )

            g.color = java.awt.Color.RED
            g.drawLine(x, diagramMarginTop, x, height - diagramMargin)
        }
    }

    private fun calculateSpeedCoordinates(speed: Int): Int {
        return height - diagramMargin - (pxPerKts * speed).toInt()
    }

    private fun calculateAltitudeCoordinates(altitude: Int): Int {
        return height - diagramMargin - (pxPerFt * altitude).toInt()
    }

    private fun calculateXcoordinate(remainingDistance: Float): Int {
        return width - diagramMargin - (pxPerNm * remainingDistance).toInt()
    }

    private fun findClosestTrajectoryPointAlongXAxis(x: Int): TrajectoryPoint? {
        val closestPoint = trajectoryPoints.minByOrNull { point ->
            val xPos = calculateXcoordinate(point.remainingDistance)
            abs(x - xPos)
        }
        return closestPoint
    }

}

