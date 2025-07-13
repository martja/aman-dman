package no.vaccsca.amandman.common

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