package no.vaccsca.amandman.presenter

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.ApplicationMode
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint

/**
 * Presenter interface that adapts based on application mode
 */
interface ModeAwarePresenterInterface : PresenterInterface {
    val applicationMode: ApplicationMode

    /**
     * Checks if a feature is available in the current mode
     */
    fun isFeatureAvailable(feature: Feature): Boolean {
        return when (feature) {
            Feature.MODIFY_SEQUENCE -> applicationMode != ApplicationMode.SLAVE
            Feature.SET_MINIMUM_SPACING -> applicationMode != ApplicationMode.SLAVE
            Feature.MANUAL_AIRCRAFT_MOVEMENT -> applicationMode != ApplicationMode.SLAVE
            Feature.RECALCULATE_SEQUENCE -> applicationMode != ApplicationMode.SLAVE
            Feature.VIEW_DESCENT_PROFILE -> applicationMode != ApplicationMode.SLAVE
            Feature.CREATE_TIMELINE -> true
            Feature.DELETE_TIMELINE -> true
            Feature.VIEW_SEQUENCE -> true
            Feature.VIEW_WEATHER -> true
        }
    }
}

enum class Feature {
    MODIFY_SEQUENCE,
    CREATE_TIMELINE,
    DELETE_TIMELINE,
    SET_MINIMUM_SPACING,
    MANUAL_AIRCRAFT_MOVEMENT,
    RECALCULATE_SEQUENCE,
    VIEW_SEQUENCE,
    VIEW_WEATHER,
    VIEW_DESCENT_PROFILE
}

/**
 * Common interface for both master and slave planner services
 */
interface PlannerServiceInterface {

    // Read-only operations available to all modes
    fun getDescentProfileForCallsign(callsign: String): List<TrajectoryPoint>?
    fun refreshWeatherData(lat: Double, lon: Double)
    fun subscribeForInbounds(icao: String)

    // Operations only available in master/local modes
    fun suggestScheduledTime(sequenceId: String, callsign: String, newScheduledTime: Instant) {
        throw UnsupportedOperationException("Operation not supported in slave mode")
    }

    fun reSchedule(sequenceId: String, callSign: String?) {
        throw UnsupportedOperationException("Operation not supported in slave mode")
    }

    fun isTimeSlotAvailable(sequenceId: String, callsign: String, newInstant: Instant): Boolean {
        throw UnsupportedOperationException("Operation not supported in slave mode")
    }

    fun setMinimumSpacing(minimumSpacingDistanceNm: Double) {
        throw UnsupportedOperationException("Operation not supported in slave mode")
    }
}
