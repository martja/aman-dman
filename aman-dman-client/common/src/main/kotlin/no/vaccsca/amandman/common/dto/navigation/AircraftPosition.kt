package no.vaccsca.amandman.common.dto.navigation

import no.vaccsca.amandman.common.LatLng

data class AircraftPosition(
    val position: LatLng,
    val altitudeFt: Int,
    val groundspeedKts: Int,
    val trackDeg: Int
)