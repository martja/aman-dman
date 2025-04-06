package org.example.entities.json


data class RegisterTimelineJson(
    val timelineId: Long,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val destinationAirports: List<String>,
)