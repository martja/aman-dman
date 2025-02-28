package org.example.presentation

import integration.AtcClientEuroScope
import TimeRangeScrollBar
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import model.entities.json.DataPackageJson
import org.example.integration.entities.TimelineAircraftJson
import org.example.integration.entities.TimelineUpdate
import org.example.model.entities.json.RegisterTimelineJson
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

        val timelinesToRegister = listOf<RegisterTimelineJson>(
            RegisterTimelineJson(
                timelineId = 1L,
                targetFixes = listOf("GSW40", "GME40"),
                viaFixes = listOf("ADOPI", "LUNIP", "ESEBA", "INREX", "RIPAM", "BELGU"),
                destinationAirports = listOf("ENGM")
            ),
            RegisterTimelineJson(
                timelineId = 2L,
                targetFixes = listOf("OBW40", "ONE40"),
                viaFixes = listOf("ADOPI", "LUNIP", "ESEBA", "INREX", "RIPAM", "BELGU"),
                destinationAirports = listOf("ENGM")
            )
        )

        AtcClientEuroScope("127.0.0.1", 12345, timelinesToRegister) { dataPackage ->
            when (dataPackage) {
                is TimelineUpdate ->
                    applicationState.arrivals = dataPackage.arrivals.map { jsonData ->
                        Arrival(
                            id = jsonData.callsign,
                            callSign = jsonData.callsign,
                            icaoType = jsonData.icaoType,
                            wakeCategory =  jsonData.wtc,
                            assignedRunway = jsonData.runway,
                            assignedStar =  jsonData.star,
                            eta = Instant.ofEpochSecond(jsonData.eta),
                            remainingDistance = jsonData.remainingDist,
                            finalFix = jsonData.finalFix,
                            flightLevel = jsonData.flightLevel,
                            pressureAltitude = jsonData.pressureAltitude,
                            groundSpeed = jsonData.groundSpeed,
                            secondsBehindPreceeding = jsonData.secondsBehindPreceeding,
                            isAboveTransAlt = jsonData.isAboveTransAlt,
                            trackedByMe = jsonData.trackedByMe,
                            timeToLoseOrGain = 0.seconds,
                            arrivalAirportIcao = "N/A",
                            viaFix = jsonData.viaFix
                        )
                    }
            }
        }

    }
}