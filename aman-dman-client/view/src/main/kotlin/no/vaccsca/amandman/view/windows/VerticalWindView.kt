package no.vaccsca.amandman.view.windows

import no.vaccsca.amandman.common.util.NumberUtils.format
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import no.vaccsca.amandman.view.util.WindBarbs
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

                WindBarbs.drawWindBarb(
                    g,
                    width - diagramMargin - 20,
                    yPos,
                    it.windVector.directionDeg,
                    it.windVector.speedKts,
                    length = 40,
                    barbMaxLength = 12,
                )

                g.drawString("FL${(it.flightLevelFt / 100.0).roundToInt().toString().padStart(3, '0')}: ${it.windVector.directionDeg.toString().padStart(3, '0')} / ${it.windVector.speedKts} kt / ${it.temperatureC} C", 30, yPos)
            }

            g.drawString("Valid " + currentProfile.time.format("yyyy/MM/dd HH:mm") + "Z", 5, height - 5)
        } ?: run {
            g.color = Color.RED
            g.drawString("No data available", 5, height - 5)
        }
    }
}
