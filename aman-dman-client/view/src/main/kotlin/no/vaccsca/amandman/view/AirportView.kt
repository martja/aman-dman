package no.vaccsca.amandman.view

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.domain.valueobjects.TimelineData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.airport.Footer
import no.vaccsca.amandman.view.airport.TimeRangeScrollBarVertical
import no.vaccsca.amandman.view.airport.TimelineScrollPane
import no.vaccsca.amandman.view.airport.TopBar
import no.vaccsca.amandman.view.airport.timeline.TimelineView
import no.vaccsca.amandman.view.components.ReloadButton
import no.vaccsca.amandman.view.entity.MainViewState
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

class AirportView(
    private val presenter: PresenterInterface,
    val airportIcao: String,
    mainViewState: MainViewState
) : JDesktopPane() {

    private val airportViewState = mainViewState.airportViewStates.value.find { it.airportIcao == airportIcao }
        ?: throw IllegalStateException("No AirportViewModel found for airport ${airportIcao}")

    val timeWindowScrollbar = TimeRangeScrollBarVertical(airportViewState.selectedTimeRange, airportViewState.availableTimeRange)
    val reloadButton = ReloadButton("Recalculate sequence for all arrivals") {
        presenter.onRecalculateSequenceClicked(airportIcao)
    }
    val westPanel = JPanel(BorderLayout()).apply {
        add(timeWindowScrollbar, BorderLayout.CENTER)
        add(reloadButton, BorderLayout.SOUTH)
    }

    val timelineScrollPane = TimelineScrollPane(airportViewState, presenter)
    val topBar = TopBar(presenter, airportViewState)
    val footer = Footer(mainViewState)

    private val landingRatesGraph = LandingRatesGraph()
    private var landingRatesFrame: JInternalFrame? = null

    private val nonSeqView = NonSeqView(airportViewState)
    private var nonSeqFrame: JInternalFrame? = null

    private val verticalWindView = VerticalWindView(presenter, airportIcao)
    private var windFrame: JInternalFrame? = null

    private var currentTime: Instant? = null

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

        mainViewState.currentClock.addListener { currentTime ->
            updateTime(currentTime)
        }

        airportViewState.openTimelines.addListener {
            updateVisibleTimelines(it)
        }

        airportViewState.events.addListener { tabData ->
            updateAmanData(tabData)
        }
    }

    private fun updateTime(currentTime: Instant) {
        val delta = if (this.currentTime != null) {
            currentTime - this.currentTime!!
        } else {
            Duration.ZERO
        }
        airportViewState.selectedTimeRange.value = TimeRange(
            airportViewState.selectedTimeRange.value.start + delta,
            airportViewState.selectedTimeRange.value.end + delta,
        )
        airportViewState.availableTimeRange.value = TimeRange(
            currentTime - airportViewState.maxHistory,
            currentTime + airportViewState.maxFuture,
        )
        this.currentTime = currentTime
    }

    private fun updateAmanData(timelineEvents: List<TimelineEvent>) {
        timeWindowScrollbar.updateTimelineEvents(timelineEvents)
        val runwayArrivals = timelineEvents.filterIsInstance<RunwayArrivalEvent>()

        val timelineData = airportViewState.availableTimelines.filter { it.title in airportViewState.openTimelines.value }
            .map { timelineConfig ->
                val leftEvents = runwayArrivals.filter { it.runway in timelineConfig.runwaysLeft }
                val rightEvents = runwayArrivals.filter { it.runway in timelineConfig.runwaysRight }
                TimelineData(
                    timelineId = timelineConfig.title,
                    left = leftEvents,
                    right = rightEvents
                )
            }

        timelineScrollPane.updateTimelineEvents(timelineData)
        landingRatesGraph.updateData(runwayArrivals)
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

    private fun updateVisibleTimelines(openIds: List<String>) {
        // Clear existing timelines
        val items = timelineScrollPane.viewport.view as JPanel
        items.components
            .filterIsInstance<TimelineView>()
            .forEach { component -> items.remove(component) }

        // Add the current timelines
        openIds.forEach { timelineId ->
            val timelineConfig = airportViewState.availableTimelines.find { it.title == timelineId }
            if (timelineConfig != null) {
                timelineScrollPane.insertTimeline(timelineConfig)
            }
        }
        repaint()
    }

    fun openPopupMenu(availableTimelines: List<TimelineConfig>, screenPos: Point) {
        timelineScrollPane.openPopupMenu(availableTimelines, screenPos)
    }

    fun openLandingRatesWindow() {
        if (landingRatesFrame == null) {
            landingRatesFrame = JInternalFrame("Landing Rates - $airportIcao", true, true, true, true).apply {
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
            nonSeqFrame = JInternalFrame("Non-Sequenced Flights - $airportIcao", true, true, true, true).apply {
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
            windFrame = JInternalFrame("Vertical Wind Profile - $airportIcao", true, true, true, true).apply {
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