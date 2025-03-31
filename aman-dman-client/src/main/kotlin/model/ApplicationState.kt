package org.example.state

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.example.integration.WindApi
import org.example.model.entities.weather.VerticalWeatherProfile
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.Timer

class ApplicationState {

    private val pcs = PropertyChangeSupport(this)

    val verticalWeatherProfile: VerticalWeatherProfile = WindApi().getVerticalProfileAtPoint(60.0, 11.0)!!

    var timeNow: Instant = Clock.System.now()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("timeNow", old, value)
        }

    fun addListener(listener: PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }

    init {
        Timer(1000) {
            timeNow = Clock.System.now()
        }.start()
    }
}
