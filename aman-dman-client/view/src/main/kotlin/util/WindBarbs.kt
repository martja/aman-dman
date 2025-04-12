package util

import java.awt.Graphics
import java.awt.Point

object WindBarbs {
    fun drawWindBarb(
        g: Graphics,
        xPos: Int,
        yPos: Int,
        windDirDeg: Int,
        windSpeedKt: Int,
        relativeToHeading: Int? = null,
        length: Int = 30,
        barbMaxLength: Int = 8,
    ) {
        val relativeWindDir =
            if (relativeToHeading == null) {
                windDirDeg
            } else {
                (windDirDeg - relativeToHeading + 180 + 360) % 360
            }

        // Calculate shaft angle (wind direction into the wind)
        val shaftAngleRad = Math.toRadians((relativeWindDir + 180 + 90).toDouble()) // Shaft points into the wind
        val shaftX = xPos + (length * Math.cos(shaftAngleRad)).toInt()
        val shaftY = yPos + (length * Math.sin(shaftAngleRad)).toInt()

        // Draw the shaft
        g.drawLine(xPos, yPos, shaftX, shaftY)
        g.drawOval(xPos - 2, yPos - 2, 4, 4)

        // Barb positioning
        val numFifties = windSpeedKt / 50
        val numTens = (windSpeedKt % 50) / 10
        val numFives = (windSpeedKt % 10) / 5

        // Each barb is drawn at 4-pixel intervals along the shaft, from the end
        val barbSpacing = barbMaxLength / 2
        var currentBarbPos = 0

        for (i in 0 until numFifties) {
            drawTriangle(g, shaftX, shaftY, shaftAngleRad, currentBarbPos, barbMaxLength)
            currentBarbPos += barbSpacing
        }

        for (i in 0 until numTens) {
            drawBarb(g, shaftX, shaftY, shaftAngleRad, currentBarbPos, barbLength = barbMaxLength)
            currentBarbPos += barbSpacing
        }

        for (i in 0 until numFives) {
            drawBarb(g, shaftX, shaftY, shaftAngleRad, currentBarbPos, barbLength = barbMaxLength / 2)
            currentBarbPos += barbSpacing
        }
    }

    fun drawBarb(g: Graphics, baseX: Int, baseY: Int, angle: Double, offset: Int, barbLength: Int) {
        val posX = baseX - (offset * Math.cos(angle)).toInt()
        val posY = baseY - (offset * Math.sin(angle)).toInt()

        val barbAngle = angle - Math.toRadians(60.0) // 60 degrees off shaft

        val barbX = posX + (barbLength * Math.cos(barbAngle)).toInt()
        val barbY = posY + (barbLength * Math.sin(barbAngle)).toInt()

        g.drawLine(posX, posY, barbX, barbY)
    }

    fun drawTriangle(g: Graphics, baseX: Int, baseY: Int, angle: Double, offset: Int, length: Int) {
        val posX = baseX - (offset * Math.cos(angle)).toInt()
        val posY = baseY - (offset * Math.sin(angle)).toInt()

        val p1 = Point(posX, posY)
        val p2 = Point(
            (posX + length * Math.cos(angle - Math.toRadians(60.0))).toInt(),
            (posY + length * Math.sin(angle - Math.toRadians(60.0))).toInt()
        )
        val p3 = Point(
            (posX + length * Math.cos(angle)).toInt(),
            (posY + length * Math.sin(angle)).toInt()
        )

        val xPoints = intArrayOf(p1.x, p2.x, p3.x)
        val yPoints = intArrayOf(p1.y, p2.y, p3.y)

        g.fillPolygon(xPoints, yPoints, 3)
    }
}
