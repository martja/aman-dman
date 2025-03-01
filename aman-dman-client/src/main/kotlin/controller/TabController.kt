package org.example.controller

import kotlinx.datetime.Instant
import model.entities.TimelineConfig
import org.example.config.SettingsManager
import org.example.integration.AtcClient
import org.example.model.TabState
import org.example.model.TimelineState
import org.example.state.ApplicationState
import org.example.state.DelayDefinition
import org.example.view.TabView
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TabController(
    private val applicationState: ApplicationState,
    private val tabState: TabState,
    private val atcClient: AtcClient,
) {
    private var tabView: TabView? = null
    val MIN_RANGE: Duration = 10.minutes

    init {
        applicationState.addListener { event ->
            if (event.propertyName == "timeNow") {
                moveTimeRange(1.seconds)
            }
        }
    }

    fun moveTimeRange(delta: Duration) {
        val newMin = tabState.selectedViewMin.plus(delta)
        val newMax = tabState.selectedViewMax.plus(delta)

        if (newMin > tabState.timelineMinTime && newMax < tabState.timelineMaxTime) {
            tabState.selectedViewMin = newMin
            tabState.selectedViewMax = newMax
        }
    }

    fun moveTimeRangeStart(delta: Duration) {
        val newStartTime = tabState.selectedViewMin.plus(delta)

        // Ensure the new start time is within bounds
        if (newStartTime > tabState.timelineMinTime && newStartTime < tabState.selectedViewMax.minus(MIN_RANGE)) {
            tabState.selectedViewMin = newStartTime
        }
    }

    fun moveTimeRangeEnd(delta: Duration) {
        val newEndTime = tabState.selectedViewMax.plus(delta)

        // Ensure the new start time is within bounds
        if (newEndTime < tabState.timelineMaxTime && newEndTime > tabState.selectedViewMin.plus(MIN_RANGE)) {
            tabState.selectedViewMax = newEndTime
        }
    }

    fun openNewTimeline(id: String) {
        SettingsManager.getSettings().timelines.get(id)?.let { timeline ->
            atcClient.registerTimeline(
                timelineId = id.hashCode().toLong(),
                targetFixes = timeline.targetFixes,
                viaFixes = timeline.viaFixes,
                destinationAirports = timeline.destinationAirports
            )
            val timelineConfig =  TimelineConfig(
                id = id.hashCode().toLong(),
                label = id,
                targetFixes = timeline.targetFixes,
                viaFixes = timeline.viaFixes,
            )
            val timelineState = TimelineState(tabState, timelineConfig)
            tabView?.addTimeline(timelineConfig, timelineState, TimelineController(timelineState))
            tabState.activeTimelines += id.hashCode().toLong()
        }
    }

    fun closeTimeline(id: Long) {
        //mainController!!.unregisterTimeline(id)
    }

    fun addDelayDefinition(name: String, from: Instant, to: Instant, runway: String) {
        applicationState.delays = listOf(DelayDefinition(name, from, to, runway))
    }

    fun setView(tabView: TabView) {
        this.tabView = tabView
        openNewTimeline("GM 19R/19L")
        openNewTimeline("GM 01L/01R")
        //openNewTimeline("VALPU | INSUV")
    }
}