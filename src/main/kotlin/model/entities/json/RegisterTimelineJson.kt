package org.example.model.entities.json


data class RegisterTimelineJson(
    val timelineId: Long,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val destinationAirports: List<String>,
)