package org.example.model.entities.estimation

import org.example.model.entities.navdata.LatLng
import kotlin.time.Duration

data class DescentSegment(
    val inbound: String,
    val position: LatLng,
    val targetAltitude: Int,
    val remainingDistance: Float,
    val remainingTime: Duration,
    val groundSpeed: Int,
    val tas: Int,
)