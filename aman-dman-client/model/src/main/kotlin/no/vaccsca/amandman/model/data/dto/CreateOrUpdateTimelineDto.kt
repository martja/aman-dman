package no.vaccsca.amandman.model.data.dto

data class CreateOrUpdateTimelineDto(
    val airportIcao: String,
    val title: String,
    val left: TimeLineSide,
    val right: TimeLineSide,
) {
    data class TimeLineSide(
        val targetRunways: List<String>,
        val targetFixes: List<String>,
    )
}