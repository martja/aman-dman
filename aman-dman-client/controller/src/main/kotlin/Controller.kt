import org.example.AmanDataService
import org.example.TimelineConfig
import org.example.TimelineOccurrence
import org.example.config.SettingsManager
import org.example.eventHandling.AmanDataListener
import org.example.eventHandling.ViewListener

class Controller(val model: AmanDataService, val view: AmanDmanMainFrame) : ViewListener, AmanDataListener {


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
        TODO("Not yet implemented")
    }

    override fun onNewAmanData(amanData: List<TimelineOccurrence>) {
        view.updateWithAmanData(amanData)
    }

}