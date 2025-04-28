package org.example.dto

data class CreateOrUpdateTimelineDto(
    val groupId: String,
    val title: String,
    val airportIcao: String,
    val runwayLeft: String,
    val runwayRight: String,
    val targetFixesLeft: List<String>,
    val targetFixesRight: List<String>,
)