package org.example.state

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.Instant
import javax.swing.Timer
import kotlin.random.Random
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
    val remainingDistance: Int,
    val eta: Instant,
    val assignedRunway: String,
    val feederFix: String,
    val timeToLoseOrGain: Duration,
    val arrivalAirportIcao: String
)

class TimelineState {

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

    val randoms = Random(11223)
    var arrivals = (0..100).toList().map { index ->
        makeFakeArrival(
            "NAX00" + index,
            Instant.now().plusSeconds(randoms.nextLong(-60*120, 60*120))
        )
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
        feederFix = "TITLA",
        assignedRunway = "19R",
        remainingDistance = 10,
        callSign = callsign,
        arrivalAirportIcao = "ENGM",
    )
}