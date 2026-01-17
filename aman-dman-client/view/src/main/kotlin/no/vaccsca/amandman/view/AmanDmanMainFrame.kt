package no.vaccsca.amandman.view

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.presenter.ViewInterface
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.ControllerInfoData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import no.vaccsca.amandman.view.forms.NewTimelineForm
import no.vaccsca.amandman.view.visualizations.DescentProfileVisualization
import no.vaccsca.amandman.view.dialogs.RunwayDialog
import no.vaccsca.amandman.view.dialogs.SpacingDialog
import no.vaccsca.amandman.view.dialogs.LogViewerDialog
import no.vaccsca.amandman.view.dialogs.RoleSelectionDialog
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class AmanDmanMainFrame : ViewInterface, JFrame("AMAN") {

    override lateinit var presenterInterface: PresenterInterface

    private val descentProfileVisualizationView = DescentProfileVisualization()

    private var newTimelineForm: JDialog? = null
    private var descentProfileDialog: JDialog? = null
    private var logsDialog: JDialog? = null
    private var airportViewsPanel: AirportViewsPanel? = null
    private var currentTime: Instant? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
    }

    override fun openWindow() {

        presenterInterface.onReloadSettingsRequested()
        airportViewsPanel = AirportViewsPanel(presenterInterface)

        setSize(1000, 800)
        setLocationRelativeTo(null)
        add(airportViewsPanel, BorderLayout.CENTER)

        setupContextMenu()

        isVisible = true
        isAlwaysOnTop = true
    }

    private fun setupContextMenu() {
        val contextMenu = JPopupMenu()

        val startMenuItem = JMenuItem("Start New Timeline Group")
        val logsMenuItem = JMenuItem("Open Logs")

        startMenuItem.addActionListener {
            RoleSelectionDialog.open(this) { icao, role ->
                presenterInterface.onNewTimelineGroup(icao, role)
            }
        }

        logsMenuItem.addActionListener {
            presenterInterface.onOpenLogsWindowClicked()
        }

        contextMenu.add(startMenuItem)
        contextMenu.add(logsMenuItem)

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                maybeShowPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                maybeShowPopup(e)
            }

            private fun maybeShowPopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    contextMenu.show(e.component, e.x, e.y)
                }
            }
        })
    }

    override fun updateMinimumSpacing(airportIcao: String, minimumSpacingNm: Double) = runOnUiThread {
        airportViewsPanel?.updateMinimumSpacing(airportIcao, minimumSpacingNm)
    }

    override fun openSelectRunwayDialog(
        runwayEvent: RunwayEvent,
        runwayOptions: Set<String>,
        onClose: (String) -> Unit
    ) = runOnUiThread {
        RunwayDialog.open(
            parent = this,
            runwayEvent = runwayEvent,
            runwayOptions = runwayOptions,
            onSubmit = onClose
        )
    }

    override fun showTimelineGroup(airportIcao: String) = runOnUiThread {
        airportViewsPanel?.changeVisibleGroup(airportIcao)
    }

    override fun updateTime(currentTime: Instant) = runOnUiThread {
        val previousTime = this.currentTime
        this.currentTime = currentTime
        val delta = if (previousTime != null) {
            currentTime - previousTime
        } else {
            0.seconds
        }
        airportViewsPanel?.updateTime(currentTime, delta)
    }

    override fun showAirportContextMenu(
        airportIcao: String,
        availableTimelines: List<TimelineConfig>,
        screenPos: Point
    ) = runOnUiThread {
        airportViewsPanel?.openPopupMenu(airportIcao, availableTimelines, screenPos)
    }

    override fun updateTab(airportIcao: String, tabData: TabData) = runOnUiThread {
        airportViewsPanel?.updateTab(airportIcao, tabData)
    }

    override fun updateTimelineGroups(timelineGroups: List<TimelineGroup>) = runOnUiThread {
        airportViewsPanel?.updateTimelineGroups(timelineGroups)
    }

    override fun updateDraggedLabel(
        timelineEvent: TimelineEvent,
        newInstant: Instant,
        isAvailable: Boolean
    ) = runOnUiThread {
        airportViewsPanel?.updateDraggedLabel(timelineEvent, newInstant, isAvailable)
    }

    override fun updateRunwayModes(
        airportIcao: String,
        runwayModes: List<Pair<String, Boolean>>
    ) = runOnUiThread {
        airportViewsPanel?.updateRunwayModes(airportIcao, runwayModes)
    }

    override fun openTimelineConfigForm(
        groupId: String,
        availableTagLayoutsDep: Set<String>,
        availableTagLayoutsArr: Set<String>,
        existingConfig: TimelineConfig?
    ) = runOnUiThread {
        if (newTimelineForm != null) {
            newTimelineForm?.isVisible = true
        } else {
            newTimelineForm = JDialog(this, "New timeline for $groupId").apply {
                defaultCloseOperation = DISPOSE_ON_CLOSE
                contentPane = NewTimelineForm(presenterInterface, groupId, existingConfig)
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }
        }
        val timelineForm = newTimelineForm?.contentPane as? NewTimelineForm
        timelineForm?.update(
            arrLayouts = availableTagLayoutsArr,
            depLayouts = availableTagLayoutsDep
        )
    }

    override fun closeTimelineForm() = runOnUiThread {
        newTimelineForm?.isVisible = false
        newTimelineForm?.dispose()
        newTimelineForm = null
    }

    override fun showMinimumSpacingDialog(icao: String, default: Double) = runOnUiThread {
        SpacingDialog.open(this, icao, default) { newValue ->
            presenterInterface.onMinimumSpacingDistanceSet(icao, newValue)
        }
    }

    override fun updateDescentTrajectory(
        callsign: String,
        trajectory: List<TrajectoryPoint>
    ) = runOnUiThread {
        val currentFlightLevel = (trajectory.first().altitude / 100.0).roundToInt()
        descentProfileDialog?.title =
            "$callsign - calculated descent profile from FL$currentFlightLevel"
        descentProfileVisualizationView.setDescentSegments(trajectory)
    }

    override fun openMetWindow(airportIcao: String) = runOnUiThread {
        airportViewsPanel?.openMetWindow(airportIcao)
    }

    override fun openLandingRatesWindow(airportIcao: String) = runOnUiThread {
        airportViewsPanel?.openLandingRatesWindow(airportIcao)
    }

    override fun openNonSequencedWindow(airportIcao: String) = runOnUiThread {
        airportViewsPanel?.openNonSequencedWindow(airportIcao)
    }

    override fun openLogsWindow() = runOnUiThread {
        if (logsDialog != null) {
            logsDialog?.isVisible = true
        } else {
            logsDialog = LogViewerDialog(this).apply {
                isVisible = true
            }
        }
    }

    override fun updateWeatherData(
        airportIcao: String,
        weather: VerticalWeatherProfile?
    ) = runOnUiThread {
        airportViewsPanel?.updateWeatherData(airportIcao, weather)
    }

    override fun openDescentProfileWindow(callsign: String) = runOnUiThread {
        if (descentProfileDialog != null) {
            descentProfileDialog?.isVisible = true
        } else {
            descentProfileDialog = JDialog(this).apply {
                add(descentProfileVisualizationView)
                defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                setLocationRelativeTo(this@AmanDmanMainFrame)
                preferredSize = Dimension(800, 600)
                isVisible = true
                pack()
            }
        }
        presenterInterface.onAircraftSelected(callsign)
    }

    override fun showErrorMessage(message: String) = runOnUiThread {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    override fun updateControllerInfo(controllerInfoData: ControllerInfoData) = runOnUiThread {
        if (controllerInfoData.callsign != null && controllerInfoData.facilityType != null) {
            this.title = "AMAN - ${controllerInfoData.callsign} (${controllerInfoData.facilityType})"
        } else {
            this.title = "AMAN"
        }
    }

    private fun runOnUiThread(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            block()
        } else {
            SwingUtilities.invokeLater {
                block()
            }
        }
    }
}