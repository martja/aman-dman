package org.example.entities.navigation

import org.example.LatLng

data class RoutePoint(
    val id: String,
    val position: LatLng,
    val isPassed: Boolean,
)

data class RemainingRoute(
    val arrivalAirportIcao: String,
    val points: List<RoutePoint>,
)