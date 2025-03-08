package org.example.state

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.Timer

class ApplicationState {

    private val pcs = PropertyChangeSupport(this)

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
