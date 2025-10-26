package no.vaccsca.amandman.model.data.config.yaml

data class AirportDataJson(
    val airports: Map<String, AirportJson>
)

data class AirportJson(
    val location: LocationJson,
    val runwayThresholds: Map<String, RunwayThresholdJson>
)

data class LocationJson(
    val latitude: Double,
    val longitude: Double,
)

data class RunwayThresholdJson(
    val location: LocationJson,
    val elevation: Float,
    val trueHeading: Float
)