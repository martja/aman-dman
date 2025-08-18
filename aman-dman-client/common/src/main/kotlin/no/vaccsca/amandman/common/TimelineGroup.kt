package no.vaccsca.amandman.common

data class TimelineGroup(
    val airportIcao: String,
    val name: String,
    val timelines: MutableList<TimelineConfig>
)