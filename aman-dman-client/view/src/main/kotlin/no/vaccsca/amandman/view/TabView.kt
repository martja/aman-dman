package no.vaccsca.amandman.view

import no.vaccsca.amandman.common.dto.TabData
import kotlinx.datetime.Clock
import no.vaccsca.amandman.common.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.common.SequenceStatus
import no.vaccsca.amandman.common.TimelineGroup
import no.vaccsca.amandman.controller.ControllerInterface
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.tabpage.TimeRangeScrollBarVertical
import no.vaccsca.amandman.view.tabpage.TimelineScrollPane
import no.vaccsca.amandman.view.tabpage.TopBar
import no.vaccsca.amandman.view.tabpage.timeline.TimelineView
import no.vaccsca.amandman.view.util.SharedValue
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TabView(
    controller: ControllerInterface,
    val airportIcao: String,
) : JPanel(BorderLayout()) {

    private val availableTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 1.hours,
            Clock.System.now() + 3.hours,
        )
    )

    private val selectedTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 10.minutes,
            Clock.System.now() + 60.minutes,
        )
    )

    val timeWindowScrollbar = TimeRangeScrollBarVertical(selectedTimeRange, availableTimeRange)
    val timelineScrollPane = TimelineScrollPane(selectedTimeRange, controller)
    val topBar = TopBar(controller)

    init {
        add(topBar, BorderLayout.NORTH)
        add(timeWindowScrollbar, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)

        val timer = Timer(1000) {
            selectedTimeRange.value = TimeRange(
                selectedTimeRange.value.start + 1.seconds,
                selectedTimeRange.value.end + 1.seconds,
            )
        }

        timer.start()
    }

    fun updateAmanData(tabData: TabData) {
        timeWindowScrollbar.updateTimelineEvents(tabData.timelinesData)
        timelineScrollPane.updateTimelineEvents(tabData.timelinesData)

        val numberOfNonSeq = tabData.timelinesData
            .flatMap { it.left + it.right }
            .filterIsInstance<RunwayArrivalEvent>()
            .count { it.sequenceStatus == SequenceStatus.FOR_MANUAL_REINSERTION }

        topBar.updateNonSeqNumbers(numberOfNonSeq)
    }

    fun updateTimelines(timelineGroup: TimelineGroup) {
        // Clear existing timelines
        val items = timelineScrollPane.viewport.view as JPanel
        items.components.forEach { component ->
            if (component is TimelineView) {
                items.remove(component)
            }
        }
        // Add the current timelines
        timelineGroup.timelines.forEach { timelineConfig ->
            timelineScrollPane.insertTimeline(timelineConfig)
        }
        repaint()
    }
}