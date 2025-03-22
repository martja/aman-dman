package org.example.model

import kotlinx.datetime.Instant
import org.example.model.entities.VerticalWeatherProfile
import org.example.state.ApplicationState
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
                "departures" -> pcs.firePropertyChange("departures", evt.oldValue, evt.newValue)
            }
        }
    }

    val timeNow: Instant
        get() = applicationState.timeNow

    var selectedViewMax: Instant = timeNow.plus(30.minutes)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("selectedViewMax", old, value)
        }

    var selectedViewMin: Instant = timeNow.minus(10.minutes)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("selectedViewMin", old, value)
        }

    var timelineMaxTime: Instant = timeNow.plus(2.hours)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("timelineMaxTime", old, value)
        }

    var timelineMinTime: Instant = timeNow.minus(1.hours)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("timelineMinTime", old, value)
        }

    val verticalWeatherProfile: VerticalWeatherProfile
        get() = applicationState.verticalWeatherProfile

    fun addListener(listener: java.beans.PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }

}