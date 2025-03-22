package org.example.model.v2

data class FlightLeg(
    val id: String,
    val length: Int,
    val speedLimit: Int?,

    val nextLeg: FlightLeg?,
)
