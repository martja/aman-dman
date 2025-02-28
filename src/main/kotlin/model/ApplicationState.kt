package org.example.state

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.Instant
import javax.swing.Timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class DelayDefinition(
    val name: String,
    val from: Instant,
    val to: Instant,
    val runway: String
)

data class Arrival(
    val id: String,
    val callSign: String,
    val icaoType: String,
    val wakeCategory: Char,
    val remainingDistance: Float,
    val eta: Instant,
    val assignedRunway: String,
    val assignedStar: String,
    val timeToLoseOrGain: Duration,
    val arrivalAirportIcao: String,
    val finalFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val secondsBehindPreceeding: Int,
    val isAboveTransAlt: Boolean,
    val trackedByMe: Boolean,
    val viaFix: String,
)

class ApplicationState {

    private val pcs = PropertyChangeSupport(this)

    var timeNow: Instant = Instant.now()

    var selectedViewMax: Instant = Instant.now().plusSeconds(60 * 30)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("selectedViewEnd", old, value)
        }

    var selectedViewMin: Instant = Instant.now().minusSeconds(60 * 10)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("selectedViewStart", old, value)
        }

    var timelineMaxTime: Instant = Instant.now().plusSeconds(60 * 60 * 2)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("latestAvailableTime", old, value)
        }

    var timelineMinTime: Instant = Instant.now().minusSeconds(60 * 60)
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("oldestAvailableTime", old, value)
        }

    var delays: List<DelayDefinition> = listOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("delaysChanged", old, value)
        }

    var arrivals: List<Arrival> = emptyList()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("arrivalsChanged", old, value)
        }

    fun addListener(listener: PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }

    init {
        Timer(1000) {
            timeNow = Instant.now()
            selectedViewMax = selectedViewMax.plusSeconds(1)
            selectedViewMin = selectedViewMin.plusSeconds(1)
            timelineMaxTime = timelineMaxTime.plusSeconds(1)
            timelineMinTime = timelineMinTime.plusSeconds(1)
        }.start()
    }
}

fun makeFakeArrival(callsign: String, eta: Instant): Arrival {
    return Arrival(
        id = "123",
        eta = eta,
        timeToLoseOrGain = 1.seconds,
        icaoType = "B738",
        wakeCategory = 'M',
        assignedRunway = "19R",
        remainingDistance = 10.0f,
        callSign = callsign,
        arrivalAirportIcao = "ENGM",
        finalFix = "TITLA",
        flightLevel = 350,
        pressureAltitude = 350,
        groundSpeed = 350,
        secondsBehindPreceeding = 0,
        isAboveTransAlt = false,
        trackedByMe = false,
        assignedStar = "TITLA",
        viaFix = "TITLA"
    )
}