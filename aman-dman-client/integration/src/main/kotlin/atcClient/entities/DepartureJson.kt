package org.example.integration.entities

data class DepartureJson(
    val callsign: String,
    val icaoType: String,
    val wakeCategory: Char,
    val estimatedDepartureTime: Long,
    val airportIcao: String,
    val runway: String,
    val sid: String,
)