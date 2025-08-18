package no.vaccsca.amandman.model.navigation

data class AircraftPosition(
    val position: LatLng,
    val altitudeFt: Int,
    val groundspeedKts: Int,
    val trackDeg: Int
)