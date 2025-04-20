import metWindow.VerticalWindView
import newTimelineWindow.NewTimelineForm
import org.example.*
import org.example.eventHandling.ViewListener
import tabpage.Footer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*


class AmanDmanMainFrame : JFrame("AMAN / DMAN") {

    private val tabPane = JTabbedPane()

    var viewListener: ViewListener? = null

    private val verticalWindView = VerticalWindView()
    val descentProfileVisualizationView = DescentProfileVisualization()

    private var newTimelineForm: JDialog? = null
    private var windDialog: JDialog? = null
    private var descentProfileDialog: JDialog? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
    }

    fun openWindow(viewListener: ViewListener) {
        this.viewListener = viewListener

        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        add(tabPane, BorderLayout.CENTER)
        add(Footer(viewListener), BorderLayout.SOUTH)

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
                        showTabContextMenu(e.x, e.y, tabIndex)
                    }
                }
            }
        })
        //isAlwaysOnTop = true
    }

    private fun showTabContextMenu(x: Int, y: Int, tabIndex: Int) {
        val popup = JPopupMenu()
        val tab = tabPane.getComponentAt(tabIndex) as TabView

        val addTimelineItem = JMenuItem("New timeline ...")
        addTimelineItem.addActionListener {
            viewListener!!.onNewTimelineClicked(tab.groupId)
        }

        val removeItem = JMenuItem("Remove tab")
        removeItem.addActionListener {
            tabPane.removeTabAt(tabIndex)
        }

        popup.add(addTimelineItem)
        popup.add(removeItem)
        popup.show(tabPane, x, y)
    }

    fun getTabByGroupId(groupId: String): TabView? {
        return tabPane.components.filterIsInstance<TabView>().find { it.groupId == groupId }
    }

    fun openTimelineConfigForm(groupId: String, existingConfig: TimelineConfig? = null) {
        if (newTimelineForm != null) {
            newTimelineForm?.isVisible = true
        } else {
            newTimelineForm = JDialog(this, "New timeline for $groupId").apply {
                defaultCloseOperation = DISPOSE_ON_CLOSE
                contentPane = NewTimelineForm(viewListener!!, groupId, existingConfig)
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }
        }
    }

    fun closeTimelineForm() {
        newTimelineForm?.isVisible = false
        newTimelineForm?.dispose()
        newTimelineForm = null
    }

    fun openMetWindow() {
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

    fun updateWeatherData(weather: VerticalWeatherProfile?) {
        verticalWindView.update(weather)
    }

    fun openDescentProfileWindow() {
        if (descentProfileDialog != null) {
            descentProfileDialog?.isVisible = true
        } else {
            descentProfileDialog = JDialog(this, "Descent profile").apply {
                add(descentProfileVisualizationView)
                defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                setLocationRelativeTo(this@AmanDmanMainFrame)
                preferredSize = Dimension(800, 600)
                isVisible = true
                pack()
            }
        }
    }

    fun updateTimelineGroups(groups: List<TimelineGroup>) {
        // Close tabs that are not in the groups
        for (i in tabPane.tabCount - 1 downTo 0) {
            val tab = tabPane.getComponentAt(i) as TabView
            if (groups.none { it.id == tab.groupId }) {
                tabPane.removeTabAt(i)
            }
        }

        // Add new tabs for groups that are not already present
        for (group in groups) {
            if (tabPane.components.none { (it as TabView).groupId == group.id }) {
                val tabView = TabView(viewListener!!, group.id)
                tabPane.addTab(group.name, tabView)
            }
        }

        // Update existing tabs with new data
        for (i in 0 until tabPane.tabCount) {
            val tab = tabPane.getComponentAt(i) as TabView
            val group = groups.find { it.id == tab.groupId }
            if (group != null) {
                tab.updateTimelines(group)
            }
        }
    }
}