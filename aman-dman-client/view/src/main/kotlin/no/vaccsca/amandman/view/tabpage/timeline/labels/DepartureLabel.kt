package no.vaccsca.amandman.view.tabpage.timeline.labels

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.DepartureOccurrence
import java.awt.Color

class DepartureLabel(
    departureOccurrence: DepartureOccurrence
) : TimelineLabel(
    departureOccurrence,
    defaultBackgroundColor = Color.decode("#83989B"),
    defaultForegroundColor = Color.BLACK,
    hoverBackgroundColor = Color.GRAY,
    hoverForegroundColor = Color.WHITE
) {
    override fun paintBorder(g: java.awt.Graphics) {
        super.paintBorder(g)
        g.color = java.awt.Color.GRAY
        g.drawRoundRect(this.visibleRect.x, visibleRect.y, visibleRect.width - 1, visibleRect.height - 1, 4, 4)
    }

    override fun updateText() {
        var output = "<html><pre>"
        val departureOccurrence = timelineOccurrence as DepartureOccurrence

        output += departureOccurrence.callsign.padEnd(8)
        output += departureOccurrence.runway.padEnd(5)
        output += departureOccurrence.sid.padEnd(9)
        output += departureOccurrence.icaoType.padEnd(5)
        output += departureOccurrence.wakeCategory.toString().padEnd(2)

        output += "</pre></html>"

        text = output
    }

    override fun getTimelinePlacement(): Instant {
        return (timelineOccurrence as DepartureOccurrence).scheduledTime
    }

}