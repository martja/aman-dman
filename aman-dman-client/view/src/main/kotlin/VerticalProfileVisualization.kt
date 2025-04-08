import org.example.DescentSegment
import java.awt.BorderLayout
import javax.swing.JPanel

class VerticalProfileVisualization : JPanel(BorderLayout()) {

    private var descentSegments: List<DescentSegment> = emptyList()

    init {
        background = java.awt.Color.DARK_GRAY
    }

    fun setDescentSegments(segments: List<DescentSegment>) {
        this.descentSegments = segments
        repaint()
    }

    override fun paintComponent(g: java.awt.Graphics) {
        super.paintComponent(g)

        if (descentSegments.isEmpty()) {
            return
        }

        val minAlt = descentSegments.minOf { it.targetAltitude }
        val maxAlt = descentSegments.maxOf { it.targetAltitude }

        val diagramMargin = 30

        val totalLengthNm = descentSegments.first().remainingDistance
        val totalLengthSeconds = descentSegments.first().remainingTime.inWholeSeconds
        val totalAirLengthNm = descentSegments.zipWithNext { a, b ->
            val legDuration = a.remainingTime.inWholeSeconds - b.remainingTime.inWholeSeconds
            val airDistance = a.tas * (legDuration / 3600.0)
            airDistance
        }.sum()

        val pxPerFt = (height - diagramMargin*2).toFloat() / (maxAlt - minAlt).toFloat()
        val pxPerNm = (width - diagramMargin*2).toFloat() / totalLengthNm
        val pxPerSecond = (width - diagramMargin*2).toFloat() / totalLengthSeconds

        var prevX = diagramMargin
        var prevTimeX = diagramMargin
        var prevY = diagramMargin
        var prevInbound: String? = null


        descentSegments.forEach {
            val yPos = (height - pxPerFt * (it.targetAltitude - minAlt)).toInt() - diagramMargin
            val xPos = width - (pxPerNm * it.remainingDistance).toInt() - diagramMargin
            val xPosTime = width - (pxPerSecond * it.remainingTime.inWholeSeconds).toInt() - diagramMargin

            // Visualize remaninging time wrt height
            g.color = java.awt.Color.GRAY
            g.drawLine(prevTimeX, prevY, xPosTime, yPos)
            prevTimeX = xPosTime

            // Visualize remaninging distance wrt height
            g.color = java.awt.Color.WHITE
            g.drawLine(prevX, prevY, xPos, yPos)
            prevX = xPos
            prevY = yPos

            if (prevInbound != it.inbound && prevInbound != null) {
                g.drawString(prevInbound, xPos, yPos - 5)
            }

            g.drawString(it.remainingTime.toString(), xPos, yPos + 15)
            g.drawString(it.groundSpeed.toString() + " kts", xPos, yPos + 15 + 12)
            g.drawString(it.targetAltitude.toString() + " ft", xPos, yPos + 15 + 10 + 12)
            prevInbound = it.inbound

            g.drawOval(xPos - 3, yPos - 3, 6, 6)
            //g.drawString("FL${(it.targetAltitude / 100.0).toInt().toString().padStart(3, '0')}: ${it.remainingDistance} nm / ${it.remainingTime}", 30, yPos)
        }
    }
}