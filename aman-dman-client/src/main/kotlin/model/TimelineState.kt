package org.example.model

import kotlinx.datetime.Instant
import model.entities.TimelineConfig
import org.example.state.Arrival
import org.example.state.DelayDefinition
import org.example.state.Departure
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
                "arrivals" -> pcs.firePropertyChange("arrivals", evt.oldValue, evt.newValue)
                "delays" -> pcs.firePropertyChange("delays", evt.oldValue, evt.newValue)
                "selectedViewStart" -> pcs.firePropertyChange("selectedViewStart", evt.oldValue, evt.newValue)
                "selectedViewEnd" -> pcs.firePropertyChange("selectedViewEnd", evt.oldValue, evt.newValue)
                "latestAvailableTime" -> pcs.firePropertyChange("latestAvailableTime", evt.oldValue, evt.newValue)
                "oldestAvailableTime" -> pcs.firePropertyChange("oldestAvailableTime", evt.oldValue, evt.newValue)
                "departures" -> pcs.firePropertyChange("departures", evt.oldValue, evt.newValue)
            }
        }
    }

    val arrivals: List<Arrival>
        get() = tabState.arrivals[timelineConfig.id] ?: listOf()

    val departures: List<Departure>
        get() = tabState.departures[timelineConfig.id] ?: listOf()

    val delays: List<DelayDefinition>
        get() = tabState.delays

    val timeNow: Instant
        get() = tabState.timeNow

    val selectedViewMax: Instant
        get() = tabState.selectedViewMax

    val selectedViewMin: Instant
        get() = tabState.selectedViewMin

    val timelineMaxTime: Instant
        get() = tabState.timelineMaxTime

    var activeTimelines: List<Long> = listOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("activeTimelines", old, value)
        }

    var sequence: HashMap<String, Duration> = hashMapOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("sequence", old, value)
        }

    fun addListener(listener: PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }
}