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
import no.vaccsca.amandman.view.tabpage.Footer
import no.vaccsca.amandman.view.windows.LandingRatesGraph
import no.vaccsca.amandman.view.windows.NewTimelineForm
import no.vaccsca.amandman.view.windows.NonSeqView
import no.vaccsca.amandman.view.windows.VerticalWindView
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.math.roundToInt
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile

class AmanDmanMainFrame : ViewInterface, JFrame("AMAN") {

    override lateinit var presenterInterface: PresenterInterface

    private val tabPane = JTabbedPane()

    private val nonSeqView = NonSeqView()
    private val verticalWindView = VerticalWindView()
    private val descentProfileVisualizationView = no.vaccsca.amandman.view.DescentProfileVisualization()
    private val landingRatesGraph = LandingRatesGraph()

    private var newTimelineForm: JDialog? = null
    private var windDialog: JDialog? = null
    private var descentProfileDialog: JDialog? = null
    private var landingRatesDialog: JDialog? = null
    private var nonSequencedDialog: JDialog? = null
    private var footer: Footer? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
    }

    override fun openWindow() {

        presenterInterface.onReloadSettingsRequested()
        footer = Footer(presenterInterface)

        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        add(tabPane, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)

        isVisible = true // Show the frame

        tabPane.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                maybeShowPopup(e)
            }

            override fun mouseReleased(e: java.awt.event.MouseEvent) {
                maybeShowPopup(e)
            }

            private fun maybeShowPopup(e: java.awt.event.MouseEvent) {
                if (e.isPopupTrigger) {
                    val tabIndex = tabPane.indexAtLocation(e.x, e.y)
                    if (tabIndex >= 0) {
                        val airportIcao = (tabPane.getComponentAt(tabIndex) as TabView).airportIcao
                        presenterInterface.onTabMenu(tabIndex, airportIcao)
                    }
                }
            }
        })
        //isAlwaysOnTop = true
    }

    override fun updateMinimumSpacing(airportIcao: String, minimumSpacingNm: Double) {
        footer?.updateMinimumSpacingSelector(minimumSpacingNm)
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

    override fun showTabContextMenu(tabIndex: Int, availableTimelines: List<TimelineConfig>) {
        val popup = JPopupMenu()
        val tab = tabPane.getComponentAt(tabIndex) as TabView

        val loadTimelineMenu = JMenu("Add timeline")
        if (availableTimelines.isEmpty()) {
            loadTimelineMenu.isEnabled = false
        } else {
            availableTimelines.sortedBy { it.title }.forEach { timeline ->
                val item = JMenuItem(timeline.title)
                item.addActionListener {
                    presenterInterface.onAddTimelineButtonClicked(tab.airportIcao, timeline)
                }
                loadTimelineMenu.add(item)
            }
        }

        val addTimelineItem = JMenuItem("Create timeline ...")
        addTimelineItem.addActionListener {
            presenterInterface.onCreateNewTimelineClicked(tab.airportIcao)
        }

        val removeItem = JMenuItem("Remove tab")
        removeItem.addActionListener {
            presenterInterface.onRemoveTab(tab.airportIcao)
        }

        popup.add(loadTimelineMenu)
        popup.add(addTimelineItem)
        popup.add(removeItem)
        popup.show(tabPane, tab.x, tab.y)
    }

    override fun updateTab(airportIcao: String, tabData: TabData) {
        tabPane.components.filterIsInstance<TabView>()
            .filter { it.airportIcao == airportIcao }
            .forEach { it.updateAmanData(tabData) }

        if (airportIcao == "ENGM") {
            val allArrivalEvents = tabData.timelinesData.flatMap { it.left + it.right }
            landingRatesGraph.updateData(allArrivalEvents)
            nonSeqView.updateNonSeqData(
                tabData.timelinesData.flatMap { it.left + it.right }
            )
        }

    }

    override fun removeTab(airportIcao: String) {
        val tabIndex = tabPane.components.indexOfFirst { (it as TabView).airportIcao == airportIcao }
        if (tabIndex >= 0) {
            tabPane.removeTabAt(tabIndex)
        }
    }

    override fun updateDraggedLabel(timelineEvent: TimelineEvent, newInstant: Instant, isAvailable: Boolean) {
        tabPane.components.filterIsInstance<TabView>()
            .forEach { it.updateDraggedLabel(timelineEvent, newInstant, isAvailable) }
    }

    override fun updateRunwayModes(airportIcao: String, runwayModes: List<Pair<String, Boolean>>) {
        tabPane.components.filterIsInstance<TabView>()
            .filter { it.airportIcao == airportIcao }
            .forEach { it.updateRunwayModes(runwayModes) }
    }

    override fun openTimelineConfigForm(groupId: String, existingConfig: TimelineConfig?) {
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

    override fun showTabContextMenu(tabIndex: Int, airportIcao: String) {
        val tab = tabPane.getComponentAt(tabIndex) as TabView
        presenterInterface.onTabMenu(tabIndex, airportIcao)
    }

    override fun openMetWindow() {
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
                preferredSize = Dimension(800, 600)
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
        windDialog?.title = "Weather for $airportIcao"
        verticalWindView.update(weather)
    }

    override fun openDescentProfileWindow() {
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
    }

    override fun updateTimelineGroups(timelineGroups: List<TimelineGroup>) {
        // Close tabs that are not in the groups
        for (i in tabPane.tabCount - 1 downTo 0) {
            val tab = tabPane.getComponentAt(i) as TabView
            if (timelineGroups.none { it.airportIcao == tab.airportIcao }) {
                tabPane.removeTabAt(i)
            }
        }

        // Add new tabs for groups that are not already present
        for (group in timelineGroups) {
            if (tabPane.components.none { (it as TabView).airportIcao == group.airportIcao }) {
                val tabView = TabView(presenterInterface, group.airportIcao)
                tabPane.addTab(group.name + " " + group.userRole, tabView)
            }
        }

        // Update existing tabs with new data
        for (i in 0 until tabPane.tabCount) {
            val tab = tabPane.getComponentAt(i) as TabView
            val group = timelineGroups.find { it.airportIcao == tab.airportIcao }
            if (group != null) {
                tab.updateTimelines(group)
            }
        }
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