package no.vaccsca.amandman.common

data class TimelineConfig(
    val title: String,
    val runwaysLeft: List<String>,
    val runwaysRight: List<String>,
    val targetFixesLeft: List<String>,
    val targetFixesRight: List<String>,
    val airportIcao: String,
)
