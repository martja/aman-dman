package model.entities

data class TimelineConfig(
    val id: Long,
    val label: String,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
)