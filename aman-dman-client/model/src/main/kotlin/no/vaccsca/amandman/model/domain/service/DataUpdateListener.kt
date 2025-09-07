package no.vaccsca.amandman.model.domain.service

import no.vaccsca.amandman.model.data.dto.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile

/**
 * Interface for handling data updates throughout the application.
 * This serves as the contract for components that need to be notified of data changes.
 */
interface DataUpdateListener {
    /**
     * Called when new timeline data is available
     */
    fun onLiveData(timelineEvents: List<TimelineEvent>)

    /**
     * Called when runway status changes for an airport
     */
    fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>)

    /**
     * Called when minimum spacing configuration changes
     */
    fun onMinimumSpacingUpdated(minimumSpacingNm: Double)

    /**
     * Called when weather data is updated
     */
    fun onWeatherDataUpdated(data: VerticalWeatherProfile?)
}