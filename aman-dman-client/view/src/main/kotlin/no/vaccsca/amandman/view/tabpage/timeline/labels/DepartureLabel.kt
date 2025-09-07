package no.vaccsca.amandman.view.tabpage.timeline.labels

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.data.dto.timelineEvent.DepartureEvent
import java.awt.Color

class DepartureLabel(
    departureEvent: DepartureEvent
) : TimelineLabel(
    departureEvent,
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
        val departureEvent = timelineEvent as DepartureEvent

        output += departureEvent.callsign.padEnd(8)
        output += departureEvent.runway.padEnd(5)
        output += departureEvent.sid.padEnd(9)
        output += departureEvent.icaoType.padEnd(5)
        output += departureEvent.wakeCategory.toString().padEnd(2)

        output += "</pre></html>"

        text = output
    }

    override fun getTimelinePlacement(): Instant {
        return (timelineEvent as DepartureEvent).scheduledTime
    }

}