package tabpage.timeline.labels

import kotlinx.datetime.Instant
import org.example.RunwayArrivalOccurrence
import org.example.TrajectoryPoint
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class ArrivalLabel(
    val arrivalOccurrence: RunwayArrivalOccurrence
) : TimelineLabel(arrivalOccurrence) {

    override fun updateText() {
        var output = "<html><pre>"
        val fixInboundOccurrence = timelineOccurrence as RunwayArrivalOccurrence

        output += fixInboundOccurrence.runway.padEnd(4)
        output += (fixInboundOccurrence.assignedStar?.substring(0, 3) ?: "").padEnd(4)
        output += fixInboundOccurrence.callsign.padEnd(9)
        output += fixInboundOccurrence.icaoType.padEnd(5)
        output += fixInboundOccurrence.wakeCategory.toString().padEnd(2)
        output += fixInboundOccurrence.descentTrajectory.first().remainingDistance.roundToInt().toString().padStart(5)
        //output += fixInboundOccurrence.viaFix.padEnd(6)

        val secondsToLoseOrGain = fixInboundOccurrence.timeToLooseOrGain?.inWholeSeconds ?: 0
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
        //output += fixInboundOccurrence.windDelay?.toString(DurationUnit.MINUTES, 1)?.padStart(6)

        output += "</pre></html>"

        text = output
    }

    override fun getBorderColor(): Color {
        if (arrivalOccurrence.basedOnNavdata) {
            return super.getBorderColor()
        }
        return Color.ORANGE
    }

    override fun getTimelinePlacement(): Instant {
        return (timelineOccurrence).time
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