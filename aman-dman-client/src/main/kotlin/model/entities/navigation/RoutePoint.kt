package org.example.model.entities.navigation

import org.example.model.entities.navdata.LatLng

data class RoutePoint(
    val id: String,
    val position: LatLng
)