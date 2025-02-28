package org.example.model.entities.json

data class AmanDmanSettings(
    val openAutomatically: Boolean,
    val timelines: Map<String, Timeline>,
    val tagLayouts: Map<String, List<TagLayoutElement>>
)

data class Timeline(
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val tagLayout: String,
    val destinationAirports: List<String>,
    val defaultTimeSpan: Int? = null
)

data class TagLayoutElement(
    val source: String,
    val width: Int,
    val defaultValue: String? = null,
    val isViaFixIndicator: Boolean? = null,
    val rightAligned: Boolean? = null
)
