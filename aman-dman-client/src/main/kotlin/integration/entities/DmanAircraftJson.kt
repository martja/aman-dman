package org.example.integration.entities

data class DmanAircraftJson(
    val callsign: String,
    val sid: String,
    val runway: String,
    val icaoType: String,
    val wakeCategory: Char,
    val estimatedDepartureTime: Long,
)