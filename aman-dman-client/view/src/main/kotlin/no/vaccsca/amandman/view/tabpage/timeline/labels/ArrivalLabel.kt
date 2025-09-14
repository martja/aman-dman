package no.vaccsca.amandman.view.tabpage.timeline.labels

import no.vaccsca.amandman.presenter.PresenterInterface
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ArrivalLabel(
    val arrivalEvent: RunwayArrivalEvent,
    val presenterInterface: PresenterInterface
) : TimelineLabel(arrivalEvent) {

    private val TTL_TTG_THRESHOLD = 10.seconds

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                maybeShowPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                maybeShowPopup(e)
            }

            private fun maybeShowPopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showPopupMenu(e)
                }
            }
        })
    }

    private fun showPopupMenu(e: MouseEvent) {
        val popup = JPopupMenu()

        val rescheduleItem = JMenuItem("Re-schedule")
        rescheduleItem.addActionListener {
            presenterInterface.onRecalculateSequenceClicked(arrivalEvent.airportIcao, arrivalEvent.callsign)
        }

        popup.add(rescheduleItem)
        popup.show(e.component, e.x, e.y)
    }


    override fun updateText() {
        var output = "<html><pre>"
        val fixInboundEvent = timelineEvent as RunwayArrivalEvent

        output += fixInboundEvent.runway.padEnd(4)
        output += (fixInboundEvent.assignedStar?.substring(0, 3) ?: "").padEnd(4)
        output += fixInboundEvent.callsign.padEnd(9)
        output += fixInboundEvent.icaoType.padEnd(5)
        output += wakeCategoryText(fixInboundEvent.wakeCategory)
        output += ttlTtgText(fixInboundEvent, 4)
        output += (fixInboundEvent.distanceToPreceding ?: fixInboundEvent.remainingDistance)?.roundToInt().toString().padStart(5)
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
    private fun ttlTtgText(fixInboundEvent: RunwayArrivalEvent, leftPadding: Int): String {
        val timeToLoseOrGain = (fixInboundEvent.scheduledTime - fixInboundEvent.estimatedTime)
        var minutesToLoseOrGainFormatted = toNormalizedMinutes(timeToLoseOrGain).toString()

        when {
            timeToLoseOrGain > TTL_TTG_THRESHOLD -> {
                minutesToLoseOrGainFormatted = "+$minutesToLoseOrGainFormatted"
                minutesToLoseOrGainFormatted = "<span style='color: yellow;'>${minutesToLoseOrGainFormatted.padStart(leftPadding, ' ')}</span>"
            }
            timeToLoseOrGain < -TTL_TTG_THRESHOLD -> {
                minutesToLoseOrGainFormatted = "<span style='color: #00ff00;'>${minutesToLoseOrGainFormatted.padStart(leftPadding, ' ')}</span>"
            }
            else -> {
                minutesToLoseOrGainFormatted = "".padStart(leftPadding)
            }
        }
        return minutesToLoseOrGainFormatted
    }

    override fun getBorderColor(): Color {
        if (!arrivalEvent.assignedStarOk) {
            return Color.ORANGE
        }
        return super.getBorderColor()
    }

    override fun getTimelinePlacement(): Instant {
        return (timelineEvent as RunwayArrivalEvent).scheduledTime
    }

    private fun toNormalizedMinutes(seconds: Duration): Int {
        val minutes = seconds.inWholeSeconds.toDouble() / 60.0

        return when {
            minutes > 0 -> ceil(minutes).toInt()
            minutes < 0 -> floor(minutes).toInt()
            else -> 0
        }
    }
}