package org.example.model

import kotlinx.datetime.Instant
import model.entities.TimelineConfig
import org.example.model.entities.weather.VerticalWeatherProfile
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.time.Duration

class TimelineState(
    private val tabState: TabState,
    private val timelineConfig: TimelineConfig
) {
    private val pcs = PropertyChangeSupport(this)

    init {
        // Listen for changes in ApplicationState
        tabState.addListener { evt ->
            when (evt.propertyName) {
                "timeNow" -> pcs.firePropertyChange("timeNow", evt.oldValue, evt.newValue)
                "selectedViewMin" -> pcs.firePropertyChange("selectedViewMin", evt.oldValue, evt.newValue)
                "selectedViewMax" -> pcs.firePropertyChange("selectedViewMax", evt.oldValue, evt.newValue)
                "timelineMaxTime" -> pcs.firePropertyChange("timelineMaxTime", evt.oldValue, evt.newValue)
                "timelineMinTime" -> pcs.firePropertyChange("timelineMinTime", evt.oldValue, evt.newValue)
            }
        }
    }

    var arrivalOccurrences: List<FixInboundOccurrence> = listOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("arrivalOccurences", old, value)
        }

    var departureOccurrences: List<DepartureOccurrence> = listOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("departureOccurrences", old, value)
        }

    val runwayDelayOccurrence: MutableList<RunwayDelayOccurrence> = mutableListOf()

    val timelineOccurrences: List<TimelineOccurrence>
        get() = arrivalOccurrences + departureOccurrences + runwayDelayOccurrence

    val timeNow: Instant
        get() = tabState.timeNow

    val selectedViewMax: Instant
        get() = tabState.selectedViewMax

    val selectedViewMin: Instant
        get() = tabState.selectedViewMin

    var sequence: HashMap<String, Duration> = hashMapOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("sequence", old, value)
        }

    val verticalWeatherProfile: VerticalWeatherProfile
        get() = tabState.verticalWeatherProfile

    fun addListener(listener: PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }

    fun addDelayDefinition(name: String, from: Instant, duration: Duration, runway: String) {
        runwayDelayOccurrence.add(RunwayDelayOccurrence(
            timelineId = 123,
            name = name,
            time = from,
            delay = duration,
            runway = runway
        ))
    }
}