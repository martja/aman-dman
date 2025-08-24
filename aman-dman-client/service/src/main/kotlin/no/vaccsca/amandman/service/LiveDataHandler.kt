package no.vaccsca.amandman.service

import no.vaccsca.amandman.integration.atcClient.entities.RunwayStatus
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent

interface LiveDataHandler {
    fun onLiveData(amanData: List<TimelineEvent>)
    fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>, minimumSpacingNm: Double)
    fun onMinimumSpacingUpdated(minimumSpacingNm: Double)
}