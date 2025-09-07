package no.vaccsca.amandman.service

import no.vaccsca.amandman.model.dto.RunwayStatus
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.weather.VerticalWeatherProfile

interface LiveDataObserver {
    fun onLiveData(amanData: List<TimelineEvent>)
    fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>)
    fun onMinimumSpacingUpdated(minimumSpacingNm: Double)
    fun onWeatherDataUpdated(data: VerticalWeatherProfile?)
}