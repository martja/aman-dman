package no.vaccsca.amandman.view

import no.vaccsca.amandman.model.data.dto.TabData
import no.vaccsca.amandman.model.domain.TimelineGroup
import no.vaccsca.amandman.presenter.PresenterInterface
import java.awt.*
import javax.swing.*

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
    fun updateTimelineGroups(groups: List<TimelineGroup>) {
        // If we have multiple groups and tabPane is not currently added, add it back
        if (groups.size > 1 && !components.contains(tabPane)) {
            // Remove the single AirportView currently displayed
            components.filterIsInstance<AirportView>().forEach { remove(it) }
            add(tabPane, BorderLayout.CENTER)
        }

        // Remove old tabs
        val currentTabs = tabPane.components.filterIsInstance<AirportView>()
        currentTabs.forEach { tab ->
            if (groups.none { it.airportIcao == tab.airportIcao }) {
                tabPane.remove(tab)
            }
        }

        // Add new tabs
        for (group in groups) {
            if (tabPane.components.filterIsInstance<AirportView>().none { it.airportIcao == group.airportIcao }) {
                val airportView = AirportView(presenterInterface, group.airportIcao)
                tabPane.addTab("${group.name} ${group.userRole}", airportView)
            }
        }

        // Update existing tabs
        tabPane.components.filterIsInstance<AirportView>().forEach { tab ->
            val group = groups.find { it.airportIcao == tab.airportIcao }
            group?.let { tab.updateTimelines(it) }
        }

        updateTabVisibility()
    }

    /** Updates data in a specific tab */
    fun updateTab(airportIcao: String, tabData: TabData) {
        visibleTabs
            .firstOrNull { it.airportIcao == airportIcao }
            ?.updateAmanData(tabData)
    }

    /** Updates dragged label for a flight */
    fun updateDraggedLabel(
        timelineEvent: no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent,
        proposedTime: kotlinx.datetime.Instant,
        isAvailable: Boolean,
    ) {
        visibleTabs
            .firstOrNull { it.airportIcao == timelineEvent.airportIcao }
            ?.updateDraggedLabel(timelineEvent, proposedTime, isAvailable)
    }

    /** Updates runway mode information for the airport */
    fun updateRunwayModes(airportIcao: String, runwayModes: List<Pair<String, Boolean>>) {
        visibleTabs
            .firstOrNull { it.airportIcao == airportIcao }
            ?.updateRunwayModes(runwayModes)
    }

    /** Switch to a specific airport tab */
    fun changeVisibleGroup(airportIcao: String) {
        if (components.contains(tabPane)) {
            for (i in 0 until tabPane.tabCount) {
                val tab = tabPane.getComponentAt(i) as AirportView
                if (tab.airportIcao == airportIcao) {
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
}