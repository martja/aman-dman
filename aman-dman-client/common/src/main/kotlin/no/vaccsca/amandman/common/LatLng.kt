package no.vaccsca.amandman.common

import kotlin.math.roundToInt

data class LatLng(
    val lat: Double,
    val lon: Double
)

fun LatLng.distanceTo(latLng: LatLng): Double {
    val lat1 = Math.toRadians(this.lat)
    val lon1 = Math.toRadians(this.lon)
    val lat2 = Math.toRadians(latLng.lat)
    val lon2 = Math.toRadians(latLng.lon)

    val dlon = lon2 - lon1
    val dlat = lat2 - lat1

    val a = Math.pow(Math.sin(dlat / 2), 2.0) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2.0)
    val c = 2 * Math.asin(Math.sqrt(a))
    val radius = 3440.065 // in nautical miles
    return radius * c
}

fun LatLng.bearingTo(latLng: LatLng): Int {
    val lat1 = java.lang.Math.toRadians(this.lat)
    val lon1 = java.lang.Math.toRadians(this.lon)
    val lat2 = java.lang.Math.toRadians(latLng.lat)
    val lon2 = java.lang.Math.toRadians(latLng.lon)

    val dlon = lon2 - lon1

    val y = java.lang.Math.sin(dlon) * java.lang.Math.cos(lat2)
    val x = java.lang.Math.cos(lat1) * java.lang.Math.sin(lat2) - java.lang.Math.sin(lat1) * java.lang.Math.cos(lat2) * java.lang.Math.cos(dlon)

    val bearing = java.lang.Math.toDegrees(java.lang.Math.atan2(y, x))

    return ((bearing + 360) % 360).roundToInt()
}