package no.vaccsca.amandman.view

import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.presenter.PresenterInterface
import java.awt.*
import javax.swing.*
import kotlin.time.Duration

/**
 * Panel that holds multiple AirportView(s).
 * If there are multiple timeline groups, shows them in tabs.
 * If there is only a single timeline group, shows the AirportView directly.
 */
class AirportViewsPanel(
    private val presenterInterface: PresenterInterface,
) : JPanel(BorderLayout()) {

    private val tabPane = JTabbedPane()

    init {
        add(tabPane, BorderLayout.CENTER)
    }

    /** Returns all currently visible AirportView(s) */
    private val visibleTabs: List<AirportView>
        get() = if (components.contains(tabPane)) {
            tabPane.components.filterIsInstance<AirportView>()
        } else {
            components.filterIsInstance<AirportView>()
        }

    /** Updates or adds tabs according to the groups */
    fun updateTimelineGroups(timelineGroups: List<TimelineGroup>) {
        // Collect all existing AirportViews (either in tabPane or directly in this panel)
        val existingViews = if (components.contains(tabPane)) {
            tabPane.components.filterIsInstance<AirportView>()
        } else {
            components.filterIsInstance<AirportView>()
        }

        // Close tabs that are not in the groups
        for (i in tabPane.tabCount - 1 downTo 0) {
            val tab = tabPane.getComponentAt(i) as AirportView
            if (timelineGroups.none { it.airport == tab.airport }) {
                tabPane.removeTabAt(i)
            }
        }

        // Add new tabs for groups that are not already present
        for (group in timelineGroups) {
            if (existingViews.none { it.airport == group.airport }) {
                val tabView = AirportView(presenterInterface, group.airport)
                tabPane.addTab(group.name + " " + group.userRole, tabView)
            } else {
                // If the view exists but is not in tabPane, add it back
                val existingView = existingViews.find { it.airport == group.airport }
                if (existingView != null && !tabPane.components.contains(existingView)) {
                    tabPane.addTab(group.name + " " + group.userRole, existingView)
                }
            }
        }

        // Update existing tabs with new data
        for (i in 0 until tabPane.tabCount) {
            val tab = tabPane.getComponentAt(i) as AirportView
            val group = timelineGroups.find { it.airport == tab.airport }
            if (group != null) {
                tab.updateVisibleTimelines(group)
            }
        }

        updateTabVisibility()
    }

    fun updateTime(currentTime: kotlinx.datetime.Instant, delta: Duration) {
        visibleTabs.forEach { it.updateTime(currentTime, delta) }
    }

    /** Updates data in a specific tab */
    fun updateTab(airportIcao: String, tabData: TabData) {
        visibleTabs
            .firstOrNull { it.airport.icao == airportIcao }
            ?.updateAmanData(tabData)
    }

    fun updateMinimumSpacing(airportIcao: String, minimumSpacingNm: Double) {
        visibleTabs
            .firstOrNull { it.airport.icao == airportIcao }
            ?.updateMinSpacingNM(minimumSpacingNm)
    }

    /** Updates dragged label for a flight */
    fun updateDraggedLabel(
        timelineEvent: no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent,
        proposedTime: kotlinx.datetime.Instant,
        isAvailable: Boolean,
    ) {
        visibleTabs
            .firstOrNull { it.airport.icao == timelineEvent.airportIcao }
            ?.updateDraggedLabel(timelineEvent, proposedTime, isAvailable)
    }

    /** Updates runway mode information for the airport */
    fun updateRunwayModes(airportIcao: String, runwayModes: List<Pair<String, Boolean>>) {
        visibleTabs
            .firstOrNull { it.airport.icao == airportIcao }
            ?.updateRunwayModes(runwayModes)
    }

    /** Switch to a specific airport tab */
    fun changeVisibleGroup(airportIcao: String) {
        if (components.contains(tabPane)) {
            for (i in 0 until tabPane.tabCount) {
                val tab = tabPane.getComponentAt(i) as AirportView
                if (tab.airport.icao == airportIcao) {
                    tabPane.selectedIndex = i
                    return
                }
            }
        }
    }

    private fun updateTabVisibility() {
        when {
            tabPane.tabCount == 0 -> {
                // No tabs — remove everything
                remove(tabPane)
                components.filterIsInstance<AirportView>().forEach { remove(it) }
            }
            tabPane.tabCount == 1 -> {
                // Single tab — remove tabPane if added, show only AirportView
                val single = tabPane.getComponentAt(0)
                remove(tabPane)
                removeAll() // ensure no leftover views
                add(single, BorderLayout.CENTER)
            }
            tabPane.tabCount > 1 -> {
                // Multiple tabs — remove single AirportView if present, show tabPane
                if (!components.contains(tabPane)) {
                    removeAll()
                    add(tabPane, BorderLayout.CENTER)
                }
            }
        }

        revalidate()
        repaint()
    }

    fun openPopupMenu(airportIcao: String, availableTimelines: List<TimelineConfig>, screenPos: Point) {
        visibleTabs.find { it.airport.icao == airportIcao }?.openPopupMenu(availableTimelines, screenPos)
    }
}