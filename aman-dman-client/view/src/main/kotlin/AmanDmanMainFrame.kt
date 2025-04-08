import metWindow.VerticalWindView
import org.example.*
import org.example.eventHandling.ViewListener
import tabpage.Footer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JTabbedPane


class AmanDmanMainFrame : JFrame("AMAN / DMAN") {

    private val tabPane = JTabbedPane()

    var viewListener: ViewListener? = null

    private val verticalWindView = VerticalWindView()
    private val descentProfileVisualizationView = VerticalProfileVisualization()

    private var windDialog: JDialog? = null
    private var descentProfileDialog: JDialog? = null

    private var selectedCallsign: String? = null

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
        //isAlwaysOnTop = true
    }

    fun addTab(name: String, timelineConfig: TimelineConfig) {
        val newTab = TabView(viewListener!!)
        tabPane.addTab(name, newTab)
        newTab.addTimeline(timelineConfig)
    }

    fun updateWithAmanData(amanData: List<TimelineOccurrence>) {
        val selectedTab = tabPane.selectedComponent as? TabView
        selectedTab?.updateAmanData(amanData)

        val selectedDescentProfile = amanData.filterIsInstance<RunwayArrivalOccurrence>().find { it.callsign == selectedCallsign }
        if (selectedDescentProfile != null) {
            descentProfileVisualizationView.setDescentSegments(selectedDescentProfile.descentProfile)
        }
    }

    fun openMetWindow() {
        if (windDialog != null) {
            windDialog?.isVisible = true
        } else {
            windDialog = JDialog(this, "Vertical wind profile").apply {
                add(verticalWindView)
                defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                setLocationRelativeTo(this@AmanDmanMainFrame)
                preferredSize = Dimension(200, 600)
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
                preferredSize = Dimension(200, 600)
                isVisible = true
                pack()
            }
        }
    }

    fun setSelectedCallsign(callsign: String?) {
        this.selectedCallsign = callsign
    }
}