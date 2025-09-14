package no.vaccsca.amandman.model.data.dto

data class AmanDmanSettingsJson(
    //val openAutomatically: Boolean,
    val connectionConfig: ConnectionConfigJson,
    val airports: Map<String, AirportJson>,
    val timelines: List<TimelineJson>,
    //val tagLayouts: Map<String, List<TagLayoutElementJson>>
)

data class AirportJson(
    val latitude: Double,
    val longitude: Double,
)

data class ConnectionConfigJson(
    val api: ConnectionConfig,
    val atcClient: ConnectionConfig
)

data class ConnectionConfig(
    val host: String,
    val port: Int?,
)

data class TimelineJson(
    val title: String,
    val runwaysLeft: List<String>,
    val runwaysRight: List<String>,
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