package org.example.entities.navigation

import org.example.LatLng

data class RoutePoint(
    val id: String,
    val position: LatLng
)

data class RemainingRoute(
    val arrivalAirportIcao: String,
    val points: List<RoutePoint>,
)