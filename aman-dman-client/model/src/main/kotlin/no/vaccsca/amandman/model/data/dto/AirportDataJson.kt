package no.vaccsca.amandman.model.data.dto

import com.fasterxml.jackson.core.JsonLocation

data class AirportDataJson(
    val airports: Map<String, AirportJson>
)

data class AirportJson(
    val location: LocationJson,
    val runways: Map<String, RunwayThresholdJson>
)

data class LocationJson(
    val latitude: Double,
    val longitude: Double,
)

data class RunwayThresholdJson(
    val id: String,
    val location: LocationJson,
    val elevation: Float,
    val trueHeading: Float
)