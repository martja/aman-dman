package org.example.controller

import integration.AtcClientEuroScope
import kotlinx.datetime.Instant
import org.example.integration.AtcClient
import org.example.integration.entities.IncomingMessageJson
import org.example.integration.entities.TimelineAircraftJson
import org.example.integration.entities.TimelineUpdate
import org.example.model.TabState
import org.example.presentation.AmanDman
import org.example.state.ApplicationState
import org.example.state.Arrival
import org.example.view.TabView
import kotlin.time.Duration.Companion.seconds

class MainController {

    private var atcClient: AtcClient? = null
    private var mainWindow: AmanDman? = null
    private val applicationState = ApplicationState()

    fun startApplication() {
        mainWindow = AmanDman(applicationState, this)
        mainWindow?.isVisible = true
        atcClient = AtcClientEuroScope("127.0.0.1", 12345, listOf(), this::handleDataPackage)

        createNewTab("Tab 1")
        createNewTab("Tab 2")
    }

    fun createNewTab(name: String) {
        val tabState = TabState(applicationState)
        val tabController = TabController(applicationState, tabState, atcClient!!)
        val tabView = TabView(tabController, tabState)
        tabController.setView(tabView)
        mainWindow?.addTab(name, tabView)
    }

    private fun handleDataPackage(incomingMessageJson: IncomingMessageJson) {
        when (incomingMessageJson) {
            is TimelineUpdate -> {
                applicationState.arrivals[incomingMessageJson.timelineId] = incomingMessageJson.arrivals.map { it.toArrival() }
            }
        }
    }

    private fun TimelineAircraftJson.toArrival() =
        Arrival(
            id = this.callsign,
            callSign = this.callsign,
            icaoType = this.icaoType,
            wakeCategory =  this.wtc,
            assignedRunway = this.runway,
            assignedStar =  this.star,
            eta = Instant.fromEpochSeconds(this.eta),
            remainingDistance = this.remainingDist,
            finalFix = this.finalFix,
            flightLevel = this.flightLevel,
            pressureAltitude = this.pressureAltitude,
            groundSpeed = this.groundSpeed,
            secondsBehindPreceeding = this.secondsBehindPreceeding,
            isAboveTransAlt = this.isAboveTransAlt,
            trackedByMe = this.trackedByMe,
            timeToLoseOrGain = 0.seconds,
            arrivalAirportIcao = "N/A",
            viaFix = this.viaFix,
            finalFixEta = Instant.fromEpochSeconds(this.finalFixEta)
        )

}