package metWindow

import org.example.VerticalWeatherProfile
import org.example.util.NumberUtils.format
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.roundToInt

class VerticalWindView: JPanel(BorderLayout()) {

    private var profile: VerticalWeatherProfile? = null

    init {
        background = Color.DARK_GRAY
    }

    fun update(weatherProfile: VerticalWeatherProfile?) {
        this.profile = weatherProfile
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        profile?.let { currentProfile ->
            val maxFl = currentProfile.weatherLayers.maxOf { it.flightLevelFt }
            val diagramMargin = 30

            val pxPerFl = (height - diagramMargin * 2).toFloat() / maxFl.toFloat()

            currentProfile.weatherLayers.forEach {
                val yPos = (height - pxPerFl * it.flightLevelFt).roundToInt() - diagramMargin
                g.color = Color.WHITE
                g.drawLine(0, yPos, 10, yPos)

                g.drawString("FL${(it.flightLevelFt / 100.0).roundToInt().toString().padStart(3, '0')}: ${it.wind.directionDeg.toString().padStart(3, '0')} / ${it.wind.speedKts} kt", 30, yPos)
            }

            g.drawString("Valid " + currentProfile.time.format("yyyy/MM/dd HH:mm") + "Z", 5, height - 5)
        } ?: run {
            g.color = Color.RED
            g.drawString("No data available", 5, height - 5)
        }
    }
}
