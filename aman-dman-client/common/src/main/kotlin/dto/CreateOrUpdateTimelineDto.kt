package org.example.dto

data class CreateOrUpdateTimelineDto(
    val groupId: String,
    val title: String,
    val airportIcao: String,
    val left: TimeLineSide,
    val right: TimeLineSide,
) {
    data class TimeLineSide(
        val targetRunways: List<String>,
        val targetFixes: List<String>,
    )
}