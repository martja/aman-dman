package no.vaccsca.amandman.model.domain.service

import no.vaccsca.amandman.model.data.dto.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile

interface LiveDataObserver {
    fun onLiveData(amanData: List<TimelineEvent>)
    fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>)
    fun onMinimumSpacingUpdated(minimumSpacingNm: Double)
    fun onWeatherDataUpdated(data: VerticalWeatherProfile?)
}