package no.vaccsca.amandman.model.domain.valueobjects

data class AircraftPosition(
    val position: LatLng,
    val altitudeFt: Int,
    val flightLevel: Int,
    val groundspeedKts: Int,
    val trackDeg: Int
)