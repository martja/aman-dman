package org.example.model

import kotlinx.datetime.Instant
import org.example.state.ApplicationState
import org.example.state.Arrival
import org.example.state.DelayDefinition
import java.beans.PropertyChangeSupport
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TabState(private val applicationState: ApplicationState) {
    private val pcs = PropertyChangeSupport(this)

    init {
        // Listen for changes in ApplicationState
        applicationState.addListener { evt ->
            when (evt.propertyName) {
                "timeNow" -> pcs.firePropertyChange("timeNow", evt.oldValue, evt.newValue)
                "arrivals" -> pcs.firePropertyChange("arrivals", evt.oldValue, evt.newValue)
                "delays" -> pcs.firePropertyChange("delays", evt.oldValue, evt.newValue)
            }
        }
    }

    val timeNow: Instant
        get() = applicationState.timeNow

    val arrivals: HashMap<Long, List<Arrival>>
        get() = applicationState.arrivals.filter { it.key in activeTimelines }.toMutableMap() as HashMap<Long, List<Arrival>>

    val delays: List<DelayDefinition>
        get() = applicationState.delays

    var activeTimelines: List<Long> = listOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("activeTimelines", old, value)
        }

    var selectedViewMax: Instant = timeNow.plus(30.minutes)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("selectedViewEnd", old, value)
        }

    var selectedViewMin: Instant = timeNow.minus(10.minutes)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("selectedViewStart", old, value)
        }

    var timelineMaxTime: Instant = timeNow.plus(2.hours)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("latestAvailableTime", old, value)
        }

    var timelineMinTime: Instant = timeNow.minus(1.hours)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("oldestAvailableTime", old, value)
        }

    fun addListener(listener: java.beans.PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }

}