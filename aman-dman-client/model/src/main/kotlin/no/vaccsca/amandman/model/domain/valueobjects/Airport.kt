package no.vaccsca.amandman.model.domain.valueobjects

data class Airport(
    val icao: String,
    val location: LatLng,
    val runways: List<RunwayInfo>,
    val stars: List<Star>,
)