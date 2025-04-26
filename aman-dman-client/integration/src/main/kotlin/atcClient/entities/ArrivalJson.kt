package org.example.integration.entities

data class ArrivalJson(
    val callsign: String,
    val icaoType: String,
    val assignedRunway: String,
    val assignedStar: String,
    val assignedDirect: String,
    val trackingController: String,
    val scratchPad: String,
    val latitude: Double,
    val longitude: Double,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val track: Int,
    val route: List<FixPointJson>,
    val arrivalAirportIcao: String,
)

data class FixPointJson(
    val name: String,
    val isOnStar: String?,
    val latitude: Double,
    val longitude: Double,
    val isPassed: Boolean,
)
