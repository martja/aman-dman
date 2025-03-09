package org.example.view.weatherWindow

import org.example.format
import org.example.integration.WindApi
import org.example.model.entities.VerticalWindProfile
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.roundToInt

class VerticalWindView : JPanel(BorderLayout()) {
    private val verticalWindProfile: VerticalWindProfile = WindApi().getVerticalProfileAtPoint(60.0, 11.0)!!

    init {
        print(verticalWindProfile)
        background = Color.GRAY
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val maxFl = verticalWindProfile.windInformation.maxOf { it.flightLevel }
        val diagramMargin = 30

        val pxPerFl = (height - diagramMargin*2).toFloat() / maxFl.toFloat()

        verticalWindProfile.windInformation.forEach {
            val yPos = (height - pxPerFl * it.flightLevel).roundToInt() - diagramMargin
            g.color = Color.BLACK
            g.drawLine(0, yPos, 10, yPos)

            g.drawString("FL${it.flightLevel.toString().padStart(3, '0')}: ${it.windDirection.toString().padStart(3, '0')} / ${it.windSpeed} kt", 30, yPos)
        }

        g.drawString("Valid " + verticalWindProfile.time.format("yyyy/MM/dd HH:mm") + "Z", 5, height - 5)
    }
}