package no.vaccsca.amandman.view

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.airport.TimeRangeScrollBarVertical
import no.vaccsca.amandman.view.airport.TimelineScrollPane
import no.vaccsca.amandman.view.airport.TopBar
import no.vaccsca.amandman.view.airport.timeline.TimelineView
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.util.SharedValue
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class AirportView(
    private val presenter: PresenterInterface,
    val airportIcao: String,
) : JPanel(BorderLayout()) {

    private val maxHistory = 20.minutes
    private val maxFuture = 2.hours

    private val availableTimeRange = SharedValue(
        initialValue = TimeRange(
            NtpClock.now() - maxHistory,
            NtpClock.now() + maxFuture,
        )
    )

    private val selectedTimeRange = SharedValue(
        initialValue = TimeRange(
            NtpClock.now() - 10.minutes,
            NtpClock.now() + 60.minutes,
        )
    )

    val timeWindowScrollbar = TimeRangeScrollBarVertical(selectedTimeRange, availableTimeRange)
    val reloadButton = JButton("⟳").apply {
        font = Font(font.name, Font.BOLD, 20)
        toolTipText = "Recalculate sequence for all arrivals"
        margin = Insets(0,0,5,0)
        preferredSize = Dimension(0, 22)
        border = BorderFactory.createEmptyBorder(1,1,6,1)
        addActionListener {
            presenter.onRecalculateSequenceClicked(airportIcao)
        }
    }
    val westPanel = JPanel(BorderLayout()).apply {
        add(timeWindowScrollbar, BorderLayout.CENTER)
        add(reloadButton, BorderLayout.SOUTH)
    }

    val timelineScrollPane = TimelineScrollPane(selectedTimeRange, availableTimeRange, presenter, airportIcao)
    val topBar = TopBar(presenter, airportIcao)

    init {
        add(topBar, BorderLayout.NORTH)
        add(westPanel, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)
    }

    fun updateTime(currentTime: Instant, delta: Duration) {
        selectedTimeRange.value = TimeRange(
            selectedTimeRange.value.start + delta,
            selectedTimeRange.value.end + delta,
        )
        availableTimeRange.value = TimeRange(
            currentTime - maxHistory,
            currentTime + maxFuture,
        )
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
        timelineEvent: TimelineEvent,
        proposedTime: Instant,
        isAvailable: Boolean,
    ) {
        val items = timelineScrollPane.viewport.view as JPanel
        items.components.filterIsInstance<TimelineView>().forEach { timelineView ->
            timelineView.updateDraggedLabel(timelineEvent, proposedTime, isAvailable)
        }
    }

    fun updateVisibleTimelines(timelineGroup: TimelineGroup) {
        // Clear existing timelines
        val items = timelineScrollPane.viewport.view as JPanel
        items.components
            .filterIsInstance<TimelineView>()
            .forEach { component -> items.remove(component) }

        // Add the current timelines
        timelineGroup.timelines.forEach { timelineConfig ->
            timelineScrollPane.insertTimeline(timelineConfig)
        }
        repaint()
    }

    fun updateMinSpacingNM(minSpacingNm: Double) {
        timelineScrollPane.updateMinimumSpacingSelection(minSpacingNm)
    }

    fun updateRunwayModes(runwayModes: List<Pair<String, Boolean>>) {
        topBar.setRunwayModes(runwayModes)
    }

    fun openPopupMenu(availableTimelines: List<TimelineConfig>, screenPos: Point) {
        timelineScrollPane.openPopupMenu(availableTimelines, screenPos)
    }
}