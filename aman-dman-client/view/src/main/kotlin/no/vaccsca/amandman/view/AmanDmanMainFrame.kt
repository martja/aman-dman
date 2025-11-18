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
import no.vaccsca.amandman.view.airport.Footer
import no.vaccsca.amandman.view.windows.LandingRatesGraph
import no.vaccsca.amandman.view.windows.NewTimelineForm
import no.vaccsca.amandman.view.windows.NonSeqView
import no.vaccsca.amandman.view.windows.VerticalWindView
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.math.roundToInt
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import java.awt.Point
import kotlin.time.Duration.Companion.seconds

class AmanDmanMainFrame : ViewInterface, JFrame("AMAN") {

    override lateinit var presenterInterface: PresenterInterface

    private val nonSeqView = NonSeqView()
    private val verticalWindView = VerticalWindView()
    private val descentProfileVisualizationView = DescentProfileVisualization()
    private val landingRatesGraph = LandingRatesGraph()

    private var newTimelineForm: JDialog? = null
    private var windDialog: JDialog? = null
    private var descentProfileDialog: JDialog? = null
    private var landingRatesDialog: JDialog? = null
    private var nonSequencedDialog: JDialog? = null
    private var footer: Footer? = null
    private var airportViewsPanel: AirportViewsPanel? = null
    private var currentTime: Instant? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
    }

    override fun openWindow() {

        presenterInterface.onReloadSettingsRequested()
        footer = Footer(presenterInterface, this)
        airportViewsPanel = AirportViewsPanel(presenterInterface)

        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        add(airportViewsPanel, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)

        isVisible = true // Show the frame
        isAlwaysOnTop = true
    }

    override fun updateMinimumSpacing(airportIcao: String, minimumSpacingNm: Double) {
        airportViewsPanel?.updateMinimumSpacing(airportIcao, minimumSpacingNm)
    }

    override fun openSelectRunwayDialog(
        runwayEvent: RunwayEvent,
        runwayOptions: Set<String>,
        onClose: (String) -> Unit
    ) {
        val dialog = JDialog(this, "Select Runway", true).apply {
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            layout = BorderLayout(10, 10)
            setLocationRelativeTo(this@AmanDmanMainFrame)
        }

        val comboBox = JComboBox(runwayOptions.toTypedArray())
        val okButton = JButton("OK")
        val cancelButton = JButton("Cancel")

        comboBox.selectedItem = runwayEvent.runway

        okButton.addActionListener {
            val selected = comboBox.selectedItem as String
            onClose(selected)
            dialog.dispose()
        }
        cancelButton.addActionListener { dialog.dispose() }

        val buttonPanel = JPanel().apply {
            add(okButton)
            add(cancelButton)
        }

        dialog.add(comboBox, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        dialog.pack()              // <-- sizes dialog exactly to its contents
        dialog.isResizable = false // <-- prevents empty extra space
        dialog.isVisible = true
    }

    override fun showTimelineGroup(airportIcao: String) {
        airportViewsPanel?.changeVisibleGroup(airportIcao)
    }

    override fun updateTime(currentTime: Instant) {
        val previousTime = this.currentTime
        this.currentTime = currentTime
        val delta = if (previousTime != null) {
            currentTime - previousTime
        } else {
            0.seconds
        }
        airportViewsPanel?.updateTime(currentTime, delta)
    }

    override fun showAirportContextMenu(airportIcao: String, availableTimelines: List<TimelineConfig>, screenPos: Point) {
        airportViewsPanel?.openPopupMenu(airportIcao, availableTimelines, screenPos)
    }

    override fun updateTab(airportIcao: String, tabData: TabData) {
        airportViewsPanel?.updateTab(airportIcao, tabData)
        val allArrivalEvents = tabData.timelinesData.flatMap { it.left + it.right }
        landingRatesGraph.updateData(airportIcao, allArrivalEvents)
        nonSeqView.updateNonSeqData(
            tabData.timelinesData.flatMap { it.left + it.right }
        )
    }

    override fun updateTimelineGroups(timelineGroups: List<TimelineGroup>) {
        airportViewsPanel?.updateTimelineGroups(timelineGroups)
    }

    override fun updateDraggedLabel(timelineEvent: TimelineEvent, newInstant: Instant, isAvailable: Boolean) {
        airportViewsPanel?.updateDraggedLabel(timelineEvent, newInstant, isAvailable)
    }

    override fun updateRunwayModes(airportIcao: String, runwayModes: List<Pair<String, Boolean>>) {
        airportViewsPanel?.updateRunwayModes(airportIcao, runwayModes)
    }

    override fun openTimelineConfigForm(
        groupId: String,
        availableTagLayoutsDep: Set<String>,
        availableTagLayoutsArr: Set<String>,
        existingConfig: TimelineConfig?
    ) {
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

    override fun closeTimelineForm() {
        newTimelineForm?.isVisible = false
        newTimelineForm?.dispose()
        newTimelineForm = null
    }

    override fun updateDescentTrajectory(callsign: String, trajectory: List<TrajectoryPoint>) {
        val currentFlightLevel = (trajectory.first().altitude / 100.0).roundToInt() // Convert to FL
        descentProfileDialog?.title = "$callsign - calculated descent profile from FL$currentFlightLevel"
        descentProfileVisualizationView.setDescentSegments(trajectory)
    }

    override fun openMetWindow(airportIcao: String) {
        if (windDialog != null) {
            windDialog?.isVisible = true
        } else {
            windDialog = JDialog(this, "Vertical wind profile").apply {
                add(verticalWindView)
                defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                setLocationRelativeTo(this@AmanDmanMainFrame)
                preferredSize = Dimension(300, 700)
                isVisible = true
                pack()
            }
        }
        verticalWindView.showAirport(airportIcao)
    }

    override fun openLandingRatesWindow() {
        if (landingRatesDialog != null) {
            landingRatesDialog?.isVisible = true
        } else {
            landingRatesDialog = JDialog(this, "Landing Rates").apply {
                // Add your landing rates visualization component here
                add(landingRatesGraph)
                defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                setLocationRelativeTo(this@AmanDmanMainFrame)
                preferredSize = Dimension(500, 300)
                isVisible = true
                pack()
            }
        }
    }

    override fun openNonSequencedWindow() {
        if (nonSequencedDialog != null) {
            nonSequencedDialog?.isVisible = true
        } else {
            nonSequencedDialog = JDialog(this, "Non-Sequenced Flights").apply {
                add(nonSeqView)
                defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                setLocationRelativeTo(this@AmanDmanMainFrame)
                preferredSize = Dimension(300, 300)
                isVisible = true
                pack()
            }
        }
    }

    override fun updateWeatherData(airportIcao: String, weather: VerticalWeatherProfile?) {
        verticalWindView.update(airportIcao, weather)
    }

    override fun openDescentProfileWindow(callsign: String) {
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

    override fun showErrorMessage(message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    override fun updateControllerInfo(controllerInfoData: ControllerInfoData) {
        if (controllerInfoData.callsign != null && controllerInfoData.facilityType != null) {
            this.title = "AMAN - ${controllerInfoData.callsign} (${controllerInfoData.facilityType})"
        } else {
            this.title = "AMAN"
        }
    }
}