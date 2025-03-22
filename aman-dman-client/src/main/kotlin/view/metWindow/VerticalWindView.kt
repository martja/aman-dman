package org.example.view.weatherWindow

import org.example.format
import org.example.state.ApplicationState
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.roundToInt

class VerticalWindView(
    val state: ApplicationState
) : JPanel(BorderLayout()) {

    init {
        background = Color.DARK_GRAY
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val maxFl = state.verticalWindProfile.windInformation.maxOf { it.flightLevel }
        val diagramMargin = 30

        val pxPerFl = (height - diagramMargin*2).toFloat() / maxFl.toFloat()

        state.verticalWindProfile.windInformation.forEach {
            val yPos = (height - pxPerFl * it.flightLevel).roundToInt() - diagramMargin
            g.color = Color.WHITE
            g.drawLine(0, yPos, 10, yPos)

            g.drawString("FL${(it.flightLevel / 100.0).roundToInt().toString().padStart(3, '0')}: ${it.windDirection.toString().padStart(3, '0')} / ${it.windSpeed} kt", 30, yPos)
        }

        g.drawString("Valid " + state.verticalWindProfile.time.format("yyyy/MM/dd HH:mm") + "Z", 5, height - 5)
    }
}