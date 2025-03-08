package org.example.integration.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = FixInboundsUpdate::class, name = "fixInboundList"),
    JsonSubTypes.Type(value = DeparturesUpdate::class, name = "departureList"),
)
sealed class IncomingMessageJson(
    open val requestId: Int,
)

data class FixInboundsUpdate(
    override val requestId: Int,
    val inbounds: List<FixInboundJson>
) : IncomingMessageJson(requestId)

data class DeparturesUpdate(
    override val requestId: Int,
    val outbounds: List<DepartureJson>
) : IncomingMessageJson(requestId)
