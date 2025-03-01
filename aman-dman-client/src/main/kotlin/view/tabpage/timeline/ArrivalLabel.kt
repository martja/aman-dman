package view.tabpage.timeline

import org.example.model.TimelineState
import org.example.state.Arrival
import java.awt.Color
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class ArrivalLabel(
    var arrival: Arrival
) : JLabel("<html><span style='color: red;'>${arrival.callSign}</span></html>") {

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                background = Color.GRAY // Change color on hover
                isOpaque = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent?) {
                foreground = Color.WHITE // Restore default color
                isOpaque = false
                repaint()
            }
        })
    }

    override fun paintBorder(g: Graphics) {
        super.paintBorder(g)
        g.color = Color.GRAY
        g.drawRoundRect(this.visibleRect.x, visibleRect.y, visibleRect.width - 1, visibleRect.height - 1, 4, 4)
    }

    fun updateText(timelineState: TimelineState) {
        var output = "<html><pre>"

        output += arrival.assignedRunway.padEnd(4)
        output += arrival.finalFix.padEnd(8)
        output += arrival.callSign.padEnd(9)
        output += arrival.icaoType.padEnd(5)
        output += arrival.wakeCategory.toString().padEnd(2)
        output += arrival.viaFix.padEnd(6)

        val secondsToLoseOrGain = timelineState.sequence[arrival.callSign]?.inWholeSeconds ?: 0
        var minutesToLoseOrGainFormatted = toNormalizedMinutes(secondsToLoseOrGain).toString()

        when {
            secondsToLoseOrGain > 0 -> {
                minutesToLoseOrGainFormatted = "+$minutesToLoseOrGainFormatted"
                minutesToLoseOrGainFormatted = "<span style='color: yellow;'>${minutesToLoseOrGainFormatted.padStart(4, ' ')}</span>"
            }
            secondsToLoseOrGain < 0 -> {
                minutesToLoseOrGainFormatted = "<span style='color: #00ff00;'>${minutesToLoseOrGainFormatted.padStart(4, ' ')}</span>"
            }
            else -> {
                minutesToLoseOrGainFormatted = "".padStart(4)
            }
        }

        output += minutesToLoseOrGainFormatted
        output += arrival.remainingDistance.roundToInt().toString().padStart(6)

        output += "</pre></html>"

        text = output
    }


    private fun toNormalizedMinutes(seconds: Long): Int {
        val minues = seconds.toDouble() / 60.0

        return when {
            minues > 0 -> ceil(minues).toInt()
            minues < 0 -> floor(minues).toInt()
            else -> 0
        }
    }
}