package org.example.state

import kotlinx.datetime.Instant
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlinx.datetime.Clock
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
    val finalFixEta: Instant,
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

    var timeNow: Instant = Clock.System.now()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("timeNow", old, value)
        }

    var delays: List<DelayDefinition> = listOf()
        set(value) {
            val old = field
            field = value
            pcs.firePropertyChange("delaysChanged", old, value)
        }

    var arrivals: HashMap<Long, List<Arrival>> = hashMapOf()
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
            timeNow = Clock.System.now()
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
        viaFix = "TITLA",
        finalFixEta = eta
    )
}