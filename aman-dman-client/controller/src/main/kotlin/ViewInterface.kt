import org.example.TimelineConfig
import org.example.TimelineGroup
import org.example.TrajectoryPoint
import org.example.VerticalWeatherProfile
import org.example.dto.TabData

interface ViewInterface {
    fun updateTimelineGroups(timelineGroups: List<TimelineGroup>)
    fun openMetWindow()
    fun updateWeatherData(weather: VerticalWeatherProfile?)
    fun openDescentProfileWindow()
    fun openTimelineConfigForm(groupId: String, existingConfig: TimelineConfig? = null)
    fun closeTimelineForm()
    fun updateDescentTrajectory(callsign: String, trajectory: List<TrajectoryPoint>)
    fun showTabContextMenu(tabIndex: Int, airportIcao: String)
    fun updateTab(airportIcao: String, tabData: TabData)
    fun removeTab(airportIcao: String)
    fun showTabContextMenu(tabIndex: Int, availableTimelines: List<TimelineConfig>)
}