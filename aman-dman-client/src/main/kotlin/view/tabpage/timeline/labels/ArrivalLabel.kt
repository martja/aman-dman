package org.example.view.tabpage.timeline.labels

import kotlinx.datetime.Instant
import org.example.model.FixInboundOccurrence
import org.example.model.RunwayArrivalOccurrence
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.DurationUnit

class ArrivalLabel(
    arrivalOccurrence: RunwayArrivalOccurrence
) : TimelineLabel(arrivalOccurrence) {

    override fun updateText() {
        var output = "<html><pre>"
        val fixInboundOccurrence = timelineOccurrence as RunwayArrivalOccurrence

        output += fixInboundOccurrence.runway.padEnd(4)
        output += fixInboundOccurrence.arrivalAirportIcao.padEnd(8)
        output += fixInboundOccurrence.callsign.padEnd(9)
        output += fixInboundOccurrence.icaoType.padEnd(5)
        output += fixInboundOccurrence.wakeCategory.toString().padEnd(2)
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