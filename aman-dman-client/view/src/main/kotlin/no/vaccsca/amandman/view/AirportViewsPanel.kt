package no.vaccsca.amandman.view

import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.entity.AirportViewState
import no.vaccsca.amandman.view.entity.MainViewState
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.JPanel
import javax.swing.JTabbedPane

/**
 * Panel that holds multiple AirportView(s).
 * If there are multiple timeline groups, shows them in tabs.
 * If there is only a single timeline group, shows the AirportView directly.
 */
class AirportViewsPanel(
    private val presenterInterface: PresenterInterface,
    private val mainViewState: MainViewState,
) : JPanel(BorderLayout()) {

    private val tabPane = JTabbedPane()

    init {
        add(tabPane, BorderLayout.CENTER)

        mainViewState.airportViewStates.addListener {
            updateTimelineGroups(it)
        }

        mainViewState.currentTab.addListener {
            if (it != null)
                changeVisibleGroup(it)
        }
    }

    /** Returns all currently visible AirportView(s) */
    private val visibleTabs: List<AirportView>
        get() = if (components.contains(tabPane)) {
            tabPane.components.filterIsInstance<AirportView>()
        } else {
            components.filterIsInstance<AirportView>()
        }

    /** Updates or adds tabs according to the groups */
    private fun updateTimelineGroups(airportViewStates: List<AirportViewState>) {
        // Collect all existing AirportViews (either in tabPane or directly in this panel)
        val existingViews = if (components.contains(tabPane)) {
            tabPane.components.filterIsInstance<AirportView>()
        } else {
            components.filterIsInstance<AirportView>()
        }

        // Close tabs that are not in the groups
        for (i in tabPane.tabCount - 1 downTo 0) {
            val tab = tabPane.getComponentAt(i) as AirportView
            if (airportViewStates.none { it.airportIcao == tab.airportIcao }) {
                tabPane.removeTabAt(i)
            }
        }

        // Add new tabs for groups that are not already present
        for (viewState in airportViewStates) {
            if (existingViews.none { it.airportIcao == viewState.airportIcao }) {
                val tabView = AirportView(presenterInterface, viewState.airportIcao, mainViewState)
                tabPane.addTab(viewState.airportIcao + " " + viewState.userRole, tabView)
            } else {
                // If the view exists but is not in tabPane, add it back
                val existingView = existingViews.find { it.airportIcao == viewState.airportIcao }
                if (existingView != null && !tabPane.components.contains(existingView)) {
                    tabPane.addTab(viewState.airportIcao + " " + viewState.userRole, existingView)
                }
            }
        }

        updateTabVisibility()
    }

    /** Switch to a specific airport tab */
    private fun changeVisibleGroup(airportIcao: String) {
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

    fun openPopupMenu(airportIcao: String, availableTimelines: List<TimelineConfig>, screenPos: Point) {
        visibleTabs.find { it.airportIcao == airportIcao }?.openPopupMenu(availableTimelines, screenPos)
    }

    fun openLandingRatesWindow(airportIcao: String) {
        visibleTabs.find { it.airportIcao == airportIcao }?.openLandingRatesWindow()
    }

    fun openNonSequencedWindow(airportIcao: String) {
        visibleTabs.find { it.airportIcao == airportIcao }?.openNonSequencedWindow()
    }

    fun openMetWindow(airportIcao: String) {
        visibleTabs.find { it.airportIcao == airportIcao }?.openMetWindow()
    }
}