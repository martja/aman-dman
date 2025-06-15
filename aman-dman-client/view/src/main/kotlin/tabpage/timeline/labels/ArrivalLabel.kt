package tabpage.timeline.labels

import kotlinx.datetime.Instant
import org.example.RunwayArrivalOccurrence
import org.example.SequenceStatus
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

        this.defaultForegroundColor = if (fixInboundOccurrence.sequenceStatus == SequenceStatus.OK) {
            Color.WHITE
        } else {
            Color.GRAY
        }

        val remainingDistance = fixInboundOccurrence.descentTrajectory.firstOrNull()?.remainingDistance ?: 0f

        output += fixInboundOccurrence.runway.padEnd(4)
        output += (fixInboundOccurrence.assignedStar?.substring(0, 3) ?: "").padEnd(4)
        output += fixInboundOccurrence.callsign.padEnd(9)
        output += fixInboundOccurrence.icaoType.padEnd(5)
        output += wakeCategoryText(fixInboundOccurrence.wakeCategory)
        output += ttlTtgText(fixInboundOccurrence, 4)
        output += remainingDistance.roundToInt().toString().padStart(5)
        //output += fixInboundOccurrence.viaFix.padEnd(6)
        output += "</pre></html>"

        text = output
    }

    private fun wakeCategoryText(wakeCategory: Char): String {
        val textStyle = when (wakeCategory) {
            'L' ->
                "color: orange;"
            'H', 'J' ->
                "color: yellow;"
            else ->
                ""
        }
        return "<span style='$textStyle'>${wakeCategory.toString().padEnd(2)}</span>"
    }

    /**
     * Formats the time to lose or gain in minutes, with special formatting for positive and negative values.
     * Positive values are shown in yellow with a '+' sign, negative values in green, and zero is shown as blank.
     */
    private fun ttlTtgText(fixInboundOccurrence: RunwayArrivalOccurrence, leftPadding: Int): String {
        val secondsToLoseOrGain = (fixInboundOccurrence.scheduledTime - fixInboundOccurrence.estimatedTime).inWholeSeconds
        var minutesToLoseOrGainFormatted = toNormalizedMinutes(secondsToLoseOrGain).toString()

        when {
            secondsToLoseOrGain > 0 -> {
                minutesToLoseOrGainFormatted = "+$minutesToLoseOrGainFormatted"
                minutesToLoseOrGainFormatted = "<span style='color: yellow;'>${minutesToLoseOrGainFormatted.padStart(leftPadding, ' ')}</span>"
            }
            secondsToLoseOrGain < 0 -> {
                minutesToLoseOrGainFormatted = "<span style='color: #00ff00;'>${minutesToLoseOrGainFormatted.padStart(leftPadding, ' ')}</span>"
            }
            else -> {
                minutesToLoseOrGainFormatted = "".padStart(leftPadding)
            }
        }
        return minutesToLoseOrGainFormatted
    }

    override fun getBorderColor(): Color {
        if (arrivalOccurrence.basedOnNavdata) {
            return super.getBorderColor()
        }
        return Color.ORANGE
    }

    override fun getTimelinePlacement(): Instant {
        return (timelineOccurrence).scheduledTime
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