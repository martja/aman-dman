import metWindow.VerticalWindView
import newTimelineWindow.NewTimelineForm
import org.example.*
import org.example.dto.TabData
import tabpage.Footer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.math.roundToInt

class AmanDmanMainFrame : ViewInterface, JFrame("AMAN / DMAN") {

    private val tabPane = JTabbedPane()

    private var controllerInterface: ControllerInterface? = null

    private val verticalWindView = VerticalWindView()
    private val descentProfileVisualizationView = DescentProfileVisualization()

    private var newTimelineForm: JDialog? = null
    private var windDialog: JDialog? = null
    private var descentProfileDialog: JDialog? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
    }

    fun openWindow(controllerInterface: ControllerInterface) {
        this.controllerInterface = controllerInterface

        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        add(tabPane, BorderLayout.CENTER)
        add(Footer(controllerInterface), BorderLayout.SOUTH)

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
                        controllerInterface.onTabMenu(tabIndex, airportIcao)
                    }
                }
            }
        })
        //isAlwaysOnTop = true
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
                    controllerInterface!!.onAddTimelineButtonClicked(tab.airportIcao, timeline)
                }
                loadTimelineMenu.add(item)
            }
        }

        val addTimelineItem = JMenuItem("Create timeline ...")
        addTimelineItem.addActionListener {
            controllerInterface!!.onCreateNewTimelineClicked(tab.airportIcao)
        }

        val removeItem = JMenuItem("Remove tab")
        removeItem.addActionListener {
            controllerInterface!!.onRemoveTab(tab.airportIcao)
        }

        popup.add(loadTimelineMenu)
        popup.add(addTimelineItem)
        popup.add(removeItem)
        popup.show(tabPane, tab.x, tab.y)
    }

    override fun updateTab(airportIcao: String, tabData: TabData) {
        tabPane.components.filterIsInstance<TabView>()
            .find { it.airportIcao == airportIcao }
            ?.updateAmanData(tabData)
    }

    override fun removeTab(airportIcao: String) {
        val tabIndex = tabPane.components.indexOfFirst { (it as TabView).airportIcao == airportIcao }
        if (tabIndex >= 0) {
            tabPane.removeTabAt(tabIndex)
        }
    }

    override fun openTimelineConfigForm(groupId: String, existingConfig: TimelineConfig?) {
        if (newTimelineForm != null) {
            newTimelineForm?.isVisible = true
        } else {
            newTimelineForm = JDialog(this, "New timeline for $groupId").apply {
                defaultCloseOperation = DISPOSE_ON_CLOSE
                contentPane = NewTimelineForm(controllerInterface!!, groupId, existingConfig)
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
        controllerInterface?.onTabMenu(tabIndex, airportIcao)
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

    override fun updateWeatherData(weather: VerticalWeatherProfile?) {
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
            if (timelineGroups.none { it.id == tab.airportIcao }) {
                tabPane.removeTabAt(i)
            }
        }

        // Add new tabs for groups that are not already present
        for (group in timelineGroups) {
            if (tabPane.components.none { (it as TabView).airportIcao == group.id }) {
                val tabView = TabView(controllerInterface!!, group.id)
                tabPane.addTab(group.name, tabView)
            }
        }

        // Update existing tabs with new data
        for (i in 0 until tabPane.tabCount) {
            val tab = tabPane.getComponentAt(i) as TabView
            val group = timelineGroups.find { it.id == tab.airportIcao }
            if (group != null) {
                tab.updateTimelines(group)
            }
        }
    }
}