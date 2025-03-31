package org.example.util

import org.example.model.entities.navdata.LatLng
import kotlin.math.*

object NavigationUtils {
    fun LatLng.interpolatePositionAlongPath(endPoint: LatLng, distanceNm: Double): LatLng {
        val radiusNm = 3440.065 // Earth radius in NM

        val lat1 = Math.toRadians(this.lat)
        val lon1 = Math.toRadians(this.lon)
        val lat2 = Math.toRadians(endPoint.lat)
        val lon2 = Math.toRadians(endPoint.lon)

        val delta = centralAngle(lat1, lon1, lat2, lon2)

        if (delta == 0.0) return this

        val fraction = distanceNm / (radiusNm * delta)

        val A = sin((1 - fraction) * delta) / sin(delta)
        val B = sin(fraction * delta) / sin(delta)

        val x = A * cos(lat1) * cos(lon1) + B * cos(lat2) * cos(lon2)
        val y = A * cos(lat1) * sin(lon1) + B * cos(lat2) * sin(lon2)
        val z = A * sin(lat1) + B * sin(lat2)

        val lat = atan2(z, sqrt(x * x + y * y))
        val lon = atan2(y, x)

        return LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
    }


    fun centralAngle(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return acos(
            sin(lat1) * sin(lat2) +
                    cos(lat1) * cos(lat2) * cos(lon2 - lon1)
        )
    }
}