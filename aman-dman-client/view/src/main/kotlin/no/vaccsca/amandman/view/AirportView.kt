package no.vaccsca.amandman.view

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.model.domain.valueobjects.Airport
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.airport.Footer
import no.vaccsca.amandman.view.airport.TimeRangeScrollBarVertical
import no.vaccsca.amandman.view.airport.TimelineScrollPane
import no.vaccsca.amandman.view.airport.TopBar
import no.vaccsca.amandman.view.airport.timeline.TimelineView
import no.vaccsca.amandman.view.components.ReloadButton
import no.vaccsca.amandman.view.entity.SharedValue
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.visualizations.LandingRatesGraph
import no.vaccsca.amandman.view.visualizations.NonSeqView
import no.vaccsca.amandman.view.visualizations.VerticalWindView
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDesktopPane
import javax.swing.JInternalFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class AirportView(
    private val presenter: PresenterInterface,
    val airport: Airport,
) : JDesktopPane() {

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
    val reloadButton = ReloadButton("Recalculate sequence for all arrivals") {
        presenter.onRecalculateSequenceClicked(airport.icao)
    }
    val westPanel = JPanel(BorderLayout()).apply {
        add(timeWindowScrollbar, BorderLayout.CENTER)
        add(reloadButton, BorderLayout.SOUTH)
    }

    val timelineScrollPane = TimelineScrollPane(selectedTimeRange, availableTimeRange, presenter, airport)
    val topBar = TopBar(presenter, airport.icao)
    val footer = Footer()

    private val landingRatesGraph = LandingRatesGraph()
    private var landingRatesFrame: JInternalFrame? = null

    private val nonSeqView = NonSeqView()
    private var nonSeqFrame: JInternalFrame? = null

    private val verticalWindView = VerticalWindView(presenter, airport.icao)
    private var windFrame: JInternalFrame? = null

    private val contentPanel = JPanel(BorderLayout()).apply {
        add(topBar, BorderLayout.NORTH)
        add(westPanel, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    init {
        add(contentPanel)
        contentPanel.setBounds(0, 0, 800, 600)

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                contentPanel.setSize(width, height)
            }
        })
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
        topBar.updateNonSeqNumbers(tabData.nonSequencedList.size)

        val allArrivalEvents = tabData.timelinesData.flatMap { it.left + it.right }
        landingRatesGraph.updateData(allArrivalEvents)
        nonSeqView.updateNonSeqData(tabData.nonSequencedList)
    }

    fun updateDraggedLabel(
        timelineEvent: TimelineEvent,
        proposedTime: Instant,
        isAvailable: Boolean,
    ) {
        val items = timelineScrollPane.viewport.view as JPanel
        items.components.filterIsInstance<TimelineView>().forEach { timelineView ->
            if (timelineView.containsEvent(timelineEvent)) {
                timelineView.updateDraggedLabel(timelineEvent, proposedTime, isAvailable)
            }
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

    fun openLandingRatesWindow() {
        if (landingRatesFrame == null) {
            landingRatesFrame = JInternalFrame("Landing Rates - ${airport.icao}", true, true, true, true).apply {
                add(landingRatesGraph)
                setSize(500, 300)
                setLocation(50, 50)
                isVisible = true
                isIconifiable = false
                frameIcon = null
                isMaximizable = false
                defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            }
            add(landingRatesFrame)
        }
        landingRatesFrame?.isVisible = true
        landingRatesFrame?.toFront()
    }

    fun openNonSequencedWindow() {
        if (nonSeqFrame == null) {
            nonSeqFrame = JInternalFrame("Non-Sequenced Flights - ${airport.icao}", true, true, true, true).apply {
                add(nonSeqView)
                setSize(450, 300)
                setLocation(100, 100)
                isVisible = true
                isIconifiable = false
                frameIcon = null
                isMaximizable = false
                defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            }
            add(nonSeqFrame)
        }
        nonSeqFrame?.isVisible = true
        nonSeqFrame?.toFront()
    }

    fun openMetWindow() {
        if (windFrame == null) {
            windFrame = JInternalFrame("Vertical Wind Profile - ${airport.icao}", true, true, true, true).apply {
                add(verticalWindView)
                setSize(330, 700)
                setLocation(150, 150)
                isVisible = true
                isIconifiable = false
                frameIcon = null
                isMaximizable = false
                defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            }
            add(windFrame)
        }
        windFrame?.isVisible = true
        windFrame?.toFront()
    }

    fun updateWeatherData(weather: VerticalWeatherProfile?) {
        verticalWindView.update(weather)
    }
}