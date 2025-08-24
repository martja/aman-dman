package no.vaccsca.amandman.controller

import no.vaccsca.amandman.integration.atcClient.entities.RunwayStatus
import no.vaccsca.amandman.integration.amanConfig.SettingsManager

/**
 * Manages runway mode state and automatically updates the view when any component changes.
 * This follows the Single Responsibility Principle and Observer Pattern.
 */
class RunwayModeStateManager(private val view: ViewInterface) {

    private val airportStates = mutableMapOf<String, RunwayModeState>()

    /**
     * Updates runway statuses for an airport and triggers view update
     */
    fun updateRunwayStatuses(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>, minimumSpacingNm: Double) {
        val possibleRunwayModes =  inferPossibleRunwayModes(runwayStatuses)
        val newState = RunwayModeState(airportIcao, runwayStatuses, minimumSpacingNm, possibleRunwayModes)

        airportStates[airportIcao] = newState
        updateView(airportIcao, newState)
    }

    /**
     * Updates minimum spacing for all airports and triggers view updates
     */
    fun updateMinimumSpacing(minimumSpacingNm: Double) {
        airportStates.forEach { (airportIcao, currentState) ->
            val updatedState = currentState.copy(minimumSpacingNm = minimumSpacingNm)
            airportStates[airportIcao] = updatedState
            updateView(airportIcao, updatedState)
        }
    }

    /**
     * Refreshes all airport states (useful when settings change)
     */
    fun refreshAllStates() {
        airportStates.forEach { (airportIcao, currentState) ->
            val possibleRunwayModes =  inferPossibleRunwayModes(currentState.runwayStatuses)
            val updatedState = currentState.copy(runwayModes = possibleRunwayModes)
            airportStates[airportIcao] = updatedState
            updateView(airportIcao, updatedState)
        }
    }

    private fun updateView(airportIcao: String, state: RunwayModeState) {
        val displayLabels = state.generateDisplayLabels()
        view.updateRunwayModes(airportIcao, displayLabels)
    }

    private fun inferPossibleRunwayModes(runwayStatuses: Map<String, RunwayStatus>): List<String> {
        val runwayNames = runwayStatuses.keys
        val groupedByDirection = runwayNames.groupBy { it.take(2) }
        return groupedByDirection.flatMap { (direction, runways) ->
            runways + if (runways.size > 1) listOf(runways.sorted().joinToString("/")) else emptyList()
        }
    }
}
