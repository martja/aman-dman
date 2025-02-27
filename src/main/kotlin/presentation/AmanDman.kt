package org.example.presentation

import TCPJsonClient
import TimeRangeScrollBar
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import domain.DataPackageJson
import org.example.presentation.tabpage.Footer
import org.example.presentation.tabpage.TimelineScrollPane
import org.example.presentation.tabpage.TopBar
import org.example.state.Arrival
import org.example.state.ApplicationState
import java.awt.BorderLayout
import java.time.Instant
import javax.swing.*
import kotlin.time.Duration.Companion.seconds


class AmanDman : JFrame("AMAN / DMAN") {
    private val applicationState = ApplicationState()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        isAlwaysOnTop = true

        layout = BorderLayout()

        add(TimeRangeScrollBar(applicationState), BorderLayout.WEST)
        add(TimelineScrollPane(applicationState), BorderLayout.CENTER)
        add(TopBar(), BorderLayout.NORTH)
        add(Footer(), BorderLayout.SOUTH)

        isVisible = true // Show the frame

        TCPJsonClient("127.0.0.1", 12345) { message ->
            val dataPackage = jacksonObjectMapper().readValue(message, DataPackageJson::class.java)

            applicationState.arrivals = dataPackage.arrivals.map {
                Arrival(
                    id = it.callsign,
                    callSign = it.callsign,
                    icaoType = it.icaoType,
                    wakeCategory =  it.wtc,
                    assignedRunway = it.runway,
                    assignedStar =  it.star,
                    eta = Instant.ofEpochSecond(it.eta),
                    remainingDistance = it.remainingDist,
                    finalFix = it.finalFix,
                    flightLevel = it.flightLevel,
                    pressureAltitude = it.pressureAltitude,
                    groundSpeed = it.groundSpeed,
                    secondsBehindPreceeding = it.secondsBehindPreceeding,
                    isAboveTransAlt = it.isAboveTransAlt,
                    trackedByMe = it.trackedByMe,
                    timeToLoseOrGain = 0.seconds,
                    arrivalAirportIcao = "N/A"
                )
            }
        }

    }
}