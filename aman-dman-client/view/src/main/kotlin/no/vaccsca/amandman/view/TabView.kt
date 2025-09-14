package no.vaccsca.amandman.view

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
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
    presenter: PresenterInterface,
    val airportIcao: String,
) : JPanel(BorderLayout()) {

    private val maxHistory = 20.minutes
    private val maxFuture = 2.hours

    private val availableTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - maxHistory,
            Clock.System.now() + maxFuture,
        )
    )

    private val selectedTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 10.minutes,
            Clock.System.now() + 60.minutes,
        )
    )

    val timeWindowScrollbar = TimeRangeScrollBarVertical(selectedTimeRange, availableTimeRange)
    val timelineScrollPane = TimelineScrollPane(selectedTimeRange, availableTimeRange, presenter)
    val topBar = TopBar(presenter)

    init {
        add(topBar, BorderLayout.NORTH)
        add(timeWindowScrollbar, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)

        val timer = Timer(1000) {
            selectedTimeRange.value = TimeRange(
                selectedTimeRange.value.start + 1.seconds,
                selectedTimeRange.value.end + 1.seconds,
            )
            availableTimeRange.value = TimeRange(
                Clock.System.now() - maxHistory,
                Clock.System.now() + maxFuture,
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

    fun updateDraggedLabel(
        callsign: String,
        proposedTime: Instant,
        isAvailable: Boolean,
    ) {
        val items = timelineScrollPane.viewport.view as JPanel
        items.components.filterIsInstance<TimelineView>().forEach { timelineView ->
            timelineView.updateDraggedLabel(callsign, proposedTime, isAvailable)
        }
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

    fun updateRunwayModes(runwayModes: List<Pair<String, Boolean>>) {
        topBar.setRunwayModes(runwayModes)
    }
}