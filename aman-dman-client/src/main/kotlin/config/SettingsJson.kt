package org.example.config

data class AmanDmanSettingsJson(
    val openAutomatically: Boolean,
    val timelines: Map<String, TimelineJson>,
    val tagLayouts: Map<String, List<TagLayoutElementJson>>
)

data class TimelineJson(
    val targetFixes: List<String>,
    val viaFixes: List<String>,
    val tagLayout: String,
    val destinationAirports: List<String>,
    val defaultTimeSpan: Int? = null
)

data class TagLayoutElementJson(
    val source: String,
    val width: Int,
    val defaultValue: String? = null,
    val isViaFixIndicator: Boolean? = null,
    val rightAligned: Boolean? = null
)
