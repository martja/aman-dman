package no.vaccsca.amandman.model.navigation

data class RoutePoint(
    val id: String,
    val position: LatLng,
    val isPassed: Boolean,
    val isOnStar: Boolean,
)

data class RemainingRoute(
    val arrivalAirportIcao: String,
    val points: List<RoutePoint>,
)