package org.example.model.entities.estimation

import org.example.model.entities.navdata.LatLng

data class DescentStep(
    val position: LatLng,
    val altitudeFt: Int,
    val groundSpeed: Int,
    val tas: Int
)