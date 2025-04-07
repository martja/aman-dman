import org.example.AmanDataService
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import org.example.VerticalWeatherProfile
import org.example.config.SettingsManager
import org.example.eventHandling.AmanDataListener
import org.example.eventHandling.ViewListener
import org.example.weather.WindApi
import javax.swing.SwingUtilities

class Controller(val model: AmanDataService, val view: AmanDmanMainFrame) : ViewListener, AmanDataListener {
    private var weatherProfile: VerticalWeatherProfile? = null

    init {
        model.connectToAtcClient()
    }

    override fun onNewTabRequested(tabId: String) {
        model.subscribeForInbounds("ENGM")

        SettingsManager.getSettings().timelines.forEach { (id, timelineJson) ->
            val timelineConfig =  TimelineConfig(
                id = id.hashCode().toLong(),
                label = id,
                targetFixLeft = timelineJson.targetFixes.first(),
                targetFixRight = timelineJson.targetFixes.last(),
                viaFixes = timelineJson.viaFixes,
                airports = timelineJson.destinationAirports,
                runwayLeft = "01L",
                runwayRight = "01R",
            )

            view.addTab(id, timelineConfig)
        }
    }

    override fun onOpenMetWindowClicked() {
        view.openMetWindow()
    }

    override fun refreshWeatherData(lat: Double, lon: Double) {
        Thread {
            val weather = WindApi().getVerticalProfileAtPoint(lat, lon)
            weatherProfile = weather
            model.updateWeatherData(weather)
            view.updateWeatherData(weather) // This is a call to the interface
        }.start()
    }

    override fun onNewAmanData(amanData: List<TimelineOccurrence>) {
        view.updateWithAmanData(amanData)
    }

}