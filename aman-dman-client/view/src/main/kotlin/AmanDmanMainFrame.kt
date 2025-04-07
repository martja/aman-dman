import metWindow.VerticalWindView
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import org.example.VerticalWeatherProfile
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
    private var dialog: JDialog? = null

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
        val newTab = TabView()
        tabPane.addTab(name, newTab)
        newTab.addTimeline(timelineConfig)
    }

    fun updateWithAmanData(amanData: List<TimelineOccurrence>) {
        val selectedTab = tabPane.selectedComponent as? TabView
        selectedTab?.updateAmanData(amanData)
    }

    fun openMetWindow() {
        if (dialog != null) {
            dialog?.isVisible = true
        } else {
            dialog = JDialog(this, "Vertical wind profile").apply {
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
}