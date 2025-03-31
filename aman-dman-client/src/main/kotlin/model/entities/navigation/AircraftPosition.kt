package org.example.model.entities.navigation

import org.example.model.entities.navdata.LatLng

data class AircraftPosition(
    val position: LatLng,
    val altitudeFt: Int,
    val groundspeedKts: Int,
    val trackDeg: Int
)