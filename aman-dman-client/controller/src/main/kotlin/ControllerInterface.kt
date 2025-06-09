import org.example.TimelineConfig
import org.example.dto.CreateOrUpdateTimelineDto

interface ControllerInterface {
    fun onReloadSettingsRequested()
    fun onCreateNewTimeline(config: CreateOrUpdateTimelineDto)
    fun onOpenMetWindowClicked()
    fun refreshWeatherData(lat: Double, lon: Double)
    fun onOpenVerticalProfileWindowClicked()
    fun onAircraftSelected(callsign: String)
    fun onEditTimelineRequested(groupId: String, timelineTitle: String)
    fun onCreateNewTimelineClicked(groupId: String)
    fun onTabMenu(tabIndex: Int, airportIcao: String)
    fun onNewTimelineGroup(airportIcao: String)
    fun onAddTimelineButtonClicked(airportIcao: String, timelineConfig: TimelineConfig)
    fun onRemoveTab(airportIcao: String)
    fun onOpenLandingRatesWindow()
}