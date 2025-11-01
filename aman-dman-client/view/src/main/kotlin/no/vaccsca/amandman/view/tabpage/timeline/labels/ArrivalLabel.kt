package no.vaccsca.amandman.view.tabpage.timeline.labels

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.LabelItem
import no.vaccsca.amandman.model.domain.valueobjects.LabelItemAlignment
import no.vaccsca.amandman.model.domain.valueobjects.LabelItemSource
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.presenter.PresenterInterface
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ArrivalLabel(
    val labelItems: List<LabelItem>,
    val arrivalEvent: RunwayArrivalEvent,
    val presenterInterface: PresenterInterface,
    hBorder: Int,
    vBorder: Int,
) : TimelineLabel(arrivalEvent, hBorder = hBorder, vBorder = vBorder) {

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
        val flight = timelineEvent as RunwayArrivalEvent

        labelItems.forEach { item ->
            output += when (item.source) {
                LabelItemSource.CALL_SIGN ->
                    item.format(flight.callsign)
                LabelItemSource.ASSIGNED_RUNWAY ->
                    item.format(flight.runway)
                LabelItemSource.ASSIGNED_STAR ->
                    item.format(flight.assignedStar ?: "")
                LabelItemSource.AIRCRAFT_TYPE ->
                    item.format(flight.icaoType)
                LabelItemSource.WAKE_CATEGORY ->
                    item.format(flight.wakeCategory, wakeCatColor(flight.wakeCategory))
                LabelItemSource.TTL_TTG ->
                    item.format(formatTtlTtgValue(flight), ttlTtgColor(flight.scheduledTime - flight.estimatedTime))
                LabelItemSource.TIME_BEHIND_PRECEDING -> {
                    val hhmm = flight.timeToPreceding?.toComponents { _, minute, second, _ -> String.format("%02d:%02d", minute, second) } ?: "--:--"
                    item.format(hhmm)
                }
                LabelItemSource.TIME_BEHIND_PRECEDING_ROUNDED -> {
                    val minutes = flight.timeToPreceding?.let { toNormalizedMinutes(it) } ?: 0
                    item.format(minutes)
                }
                LabelItemSource.REMAINING_DISTANCE ->
                    item.format(flight.remainingDistance.roundToInt())
                LabelItemSource.DISTANCE_BEHIND_PRECEDING ->
                    item.format((flight.distanceToPreceding ?: flight.remainingDistance).roundToInt())
                LabelItemSource.DIRECT_ROUTING ->
                    item.format(flight.assignedDirect)
                LabelItemSource.SCRATCH_PAD ->
                    item.format(flight.scratchPad ?: "")
                LabelItemSource.ESTIMATED_LANDING_TIME ->
                    item.format(SimpleDateFormat("HH:mm").format(flight.estimatedTime.epochSeconds * 1000))
                LabelItemSource.GROUND_SPEED ->
                    item.format(flight.groundSpeed)
                LabelItemSource.GROUND_SPEED_10 ->
                    item.format((flight.groundSpeed / 10) * 10)
                LabelItemSource.ALTITUDE ->
                    item.format(flight.pressureAltitude)
            }
        }

        output += "</pre></html>"

        text = output
    }

    private fun wakeCatColor(wakeCategory: Char): String? =
        when (wakeCategory) {
            'L' ->
                "orange"
            'H', 'J' ->
                "yellow"
            else ->
                null
        }

    private fun ttlTtgColor(timeToLoseOrGain: Duration): String? =
        when {
            timeToLoseOrGain > TTL_TTG_THRESHOLD ->
                "yellow"
            timeToLoseOrGain < -TTL_TTG_THRESHOLD ->
                "#00ff00"
            else ->
                null
        }

    /**
     * Formats the time to lose or gain in minutes. Positive values are prefixed with a '+' sign,
     * negative values are shown as is, and values within the threshold are shown as blank.
     */
    private fun formatTtlTtgValue(fixInboundEvent: RunwayArrivalEvent): String {
        val timeToLoseOrGain = (fixInboundEvent.scheduledTime - fixInboundEvent.estimatedTime)
        val minutesToLoseOrGain = toNormalizedMinutes(timeToLoseOrGain)
        return when {
            timeToLoseOrGain > TTL_TTG_THRESHOLD ->
                "+$minutesToLoseOrGain"
            timeToLoseOrGain < -TTL_TTG_THRESHOLD ->
                minutesToLoseOrGain.toString()
            else ->
                ""
        }
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

    private fun LabelItem.format(value: Any?, color: String? = null): String {
        val originalValueAsString = value?.toString() ?: defaultValue ?: ""
        val maxCharacters = this.width.coerceAtMost(this.maxLength ?: Int.MAX_VALUE)
        val truncatedValue =
            if (originalValueAsString.length > maxCharacters) {
                originalValueAsString.substring(0, maxCharacters)
            } else {
                originalValueAsString
            }

        val paddedValue = when (this.alignment) {
            null, LabelItemAlignment.LEFT -> truncatedValue.padEnd(width)
            LabelItemAlignment.CENTER -> truncatedValue.padStart(((width - truncatedValue.length) / 2) + truncatedValue.length).padEnd(width)
            LabelItemAlignment.RIGHT -> truncatedValue.padStart(width)
        }

        return if (color != null) {
            "<span style='color: $color;'>${paddedValue}</span>"
        } else {
            paddedValue
        }
    }
}