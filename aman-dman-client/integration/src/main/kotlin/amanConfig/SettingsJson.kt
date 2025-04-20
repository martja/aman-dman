package org.example.config

data class AmanDmanSettingsJson(
    val openAutomatically: Boolean,
    val timelines: Map<String, TimelineJson>,
    val tagLayouts: Map<String, List<TagLayoutElementJson>>
)

data class TimelineJson(
    val title: String,
    val targetFixesLeft: List<String>,
    val targetFixesRight: List<String>,
    val airportIcao: String,
)

data class TagLayoutElementJson(
    val source: String,
    val width: Int,
    val defaultValue: String? = null,
    val isViaFixIndicator: Boolean? = null,
    val rightAligned: Boolean? = null
)
