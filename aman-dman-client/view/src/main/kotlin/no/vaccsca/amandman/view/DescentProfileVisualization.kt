package no.vaccsca.amandman.view

import no.vaccsca.amandman.common.TrajectoryPoint
import no.vaccsca.amandman.view.util.WindBarbs
import java.awt.BorderLayout
import java.awt.Color
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
        background = Color(40, 40, 40)

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

    private val diagramMargin = 40
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
            g.color = Color(60, 60, 60)
            g.drawLine(50, yPos, width - diagramMargin, yPos)
            g.drawString("FL" + (i / 100).toString().padStart(3, '0'), 5, yPos + 5)
        }

        var prevX = diagramMargin
        var prevY = diagramMarginTop
        var prevGsY: Int? = null
        var prevTasY: Int? = null

        var distFromPreviousBarb = 0

        trajectoryPoints.forEach {
            val x = calculateXcoordinate(it.remainingDistance)
            val yPosAlt = calculateAltitudeCoordinates(it.altitude)
            val yPosGs = calculateSpeedCoordinates(it.groundSpeed)
            val yPosTas = calculateSpeedCoordinates(it.tas)

            // Visualize true airspeed
            g.color = Color.YELLOW
            prevTasY?.let { y -> g.drawLine(prevX, y, x, yPosTas) }

            // Visualize ground speed
            g.color = Color.GREEN
            prevGsY?.let { y -> g.drawLine(prevX, y, x, yPosGs) }

            // Visualize remaining distance wrt height
            g.color = Color.MAGENTA
            g.drawLine(prevX, prevY, x, yPosAlt)

            g.color = Color.WHITE
            it.fixId?.let { fixId ->
                g.drawOval(x - 3, yPosAlt - 3, 6, 6)
                g.drawString(fixId, x, yPosAlt - 5)
                g.drawString(it.groundSpeed.toString() + " kts", x, yPosGs - 5)
            }


            if (distFromPreviousBarb > BARB_SPACING) {
                distFromPreviousBarb = 0
                WindBarbs.drawWindBarb(g, x, 40, it.wind.directionDeg, it.wind.speedKts, relativeToHeading = it.heading + 90 )
            } else {
                distFromPreviousBarb += (x - prevX)
            }

            prevX = x
            prevY = yPosAlt
            prevGsY = yPosGs
            prevTasY = yPosTas
        }

        hoveredPoint?.let {
            val x = calculateXcoordinate(it.remainingDistance)

            val tailwindComponent = it.groundSpeed - it.tas
            g.drawString(
                if (tailwindComponent > 0) "+${tailwindComponent} kt" else "${tailwindComponent} kt",
                x + 5,
                height - diagramMargin - 20
            )

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

