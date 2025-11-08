package no.vaccsca.amandman.view.airport

import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.airport.timeline.TimelineView
import no.vaccsca.amandman.view.util.SharedValue
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Point
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import kotlin.math.pow
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


class TimelineScrollPane(
    val selectedTimeRange: SharedValue<TimeRange>,
    val availableTimeRange: SharedValue<TimeRange>,
    val presenterInterface: PresenterInterface,
    val airportIcao: String,
) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    init {
        val items = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL
        viewport.add(items)

        viewport.view.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) = maybeShowPopup(e)
            override fun mouseReleased(e: java.awt.event.MouseEvent) = maybeShowPopup(e)

            private fun maybeShowPopup(e: java.awt.event.MouseEvent) {
                if (e.isPopupTrigger) {
                    val converted = javax.swing.SwingUtilities.convertPoint(e.component, e.point, viewport)
                    presenterInterface.onTabMenu(airportIcao, converted)
                }
            }
        })
    }

    fun insertTimeline(timelineConfig: TimelineConfig) {
        val tl = TimelineView(timelineConfig, selectedTimeRange, presenterInterface)
        val items = viewport.view as JPanel

        // Remove the previous glue (assumes it’s always the last component and a JLabel)
        if (items.componentCount > 0) {
            val last = items.getComponent(items.componentCount - 1)
            if (last is JLabel) {
                items.remove(last)
            }
        }

        val gbc = GridBagConstraints().apply {
            gridx = items.componentCount
            weightx = 0.0
            weighty = 1.0
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.VERTICAL
        }
        items.add(tl, gbc)

        // Add new glue at the end
        val glue = JLabel()
        val glueConstraints = GridBagConstraints().apply {
            gridx = items.componentCount
            weightx = 1.0
            weighty = 0.0
            fill = GridBagConstraints.BOTH
        }
        items.add(glue, glueConstraints)

        items.revalidate()
        items.repaint()
    }


    fun updateTimelineEvents(timelineData: List<TimelineData>) {
        val items = viewport.view as JPanel
        timelineData.forEach {
           items.components.filterIsInstance<TimelineView>().forEach { timelineView ->
                if (timelineView.timelineConfig.title == it.timelineId) {
                    timelineView.updateTimelineData(it)
                }
            }
        }
    }

    // Zoom when using scrollwheel
    override fun processMouseWheelEvent(e: java.awt.event.MouseWheelEvent) {
        // Check if Shift is down -> horizontal scroll
        if (e.isShiftDown) {
            // Horizontal scroll
            val hBar = horizontalScrollBar
            val increment = hBar.unitIncrement * e.wheelRotation
            hBar.value += increment
        } else {
            // Vertical scroll -> zoom
            val currentRange = selectedTimeRange.value
            val rangeDuration = currentRange.end - currentRange.start
            val zoomFactor = 1.1.pow(e.wheelRotation.toDouble())
            val newDuration = (rangeDuration * zoomFactor).coerceAtLeast(1.seconds)
            val centerTime = currentRange.start + rangeDuration / 2
            val newEnd = centerTime + newDuration / 2

            if (newEnd > availableTimeRange.value.end || newEnd < currentRange.start + 1.seconds || newDuration < 10.minutes) {
                return
            }

            selectedTimeRange.value = TimeRange(currentRange.start, newEnd)
        }

        e.consume()
    }

    fun openPopupMenu(availableTimelines: List<TimelineConfig>, screenPos: Point) {
        val popup = JPopupMenu()

        val timelinesMenu = JMenu("Add timeline")
        availableTimelines.sortedBy { it.title }.forEach { timeline ->
            val item = JMenuItem(timeline.title)
            item.addActionListener {
                presenterInterface.onAddTimelineButtonClicked(airportIcao, timeline)
            }
            timelinesMenu.add(item)
        }

        val customTimelineItem = JMenuItem("Custom ...")
        customTimelineItem.addActionListener {
            presenterInterface.onCreateNewTimelineClicked(airportIcao)
        }
        timelinesMenu.add(customTimelineItem)

        val closeItem = JMenuItem("Close airport view")
        closeItem.addActionListener {
            presenterInterface.onRemoveTab(airportIcao)
        }

        popup.add(timelinesMenu)
        popup.add(closeItem)
        popup.show(this, screenPos.x, screenPos.y)
    }
}
