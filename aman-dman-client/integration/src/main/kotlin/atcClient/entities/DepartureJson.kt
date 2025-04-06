package org.example.integration.entities

data class DepartureJson(
    val callsign: String,
    val sid: String,
    val runway: String,
    val icaoType: String,
    val wakeCategory: Char,
    val estimatedDepartureTime: Long,
)