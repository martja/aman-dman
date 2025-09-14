package no.vaccsca.amandman.model.data.dto.sharedState

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayDelayEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent

data class SharedStateJson<T>(
    val lastUpdate: Instant,
    val data: T
)

data class SharedStateEventJson(
    val type: String,
    @param:JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "type"
    )
    @param:JsonSubTypes(
        JsonSubTypes.Type(value = RunwayArrivalEvent::class, name = "runwayArrival"),
        JsonSubTypes.Type(value = DepartureEvent::class, name = "runwayDeparture"),
        JsonSubTypes.Type(value = RunwayDelayEvent::class, name = "runwayDelay")
    )
    val event: TimelineEvent
)
