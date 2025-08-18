package no.vaccsca.amandman.model

import no.vaccsca.amandman.model.navigation.LatLng
import no.vaccsca.amandman.model.weather.Wind
import kotlin.time.Duration

data class TrajectoryPoint(
    val fixId: String?,
    val position: LatLng,
    val altitude: Int,
    val remainingDistance: Float,
    val remainingTime: Duration,
    val groundSpeed: Int,
    val tas: Int,
    val ias: Int,
    val wind: Wind,
    val heading: Int,
)