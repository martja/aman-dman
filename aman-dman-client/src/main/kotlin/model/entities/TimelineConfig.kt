package model.entities

data class TimelineConfig(
    val label: String,
    val targetFixes: List<String>,
    val viaFixes: List<String>,
)