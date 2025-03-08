package model.entities

data class TimelineConfig(
    val id: Long,
    val label: String,
    val targetFixLeft: String,
    val targetFixRight: String?,
    val viaFixes: List<String>,
    val runwayLeft: String? = null,
    val runwayRight: String? = null,
    val airports: List<String>,
)