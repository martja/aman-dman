package no.vaccsca.amandman.model.domain.valueobjects

data class Airport(
    val icao: String,
    val location: LatLng,
    val runways: Map<String, RunwayThreshold>,
)

data class RunwayThreshold(
    val id: String,
    val latLng: LatLng,
    val elevation: Float,
    val trueHeading: Float,
    val stars: List<Star>,
)