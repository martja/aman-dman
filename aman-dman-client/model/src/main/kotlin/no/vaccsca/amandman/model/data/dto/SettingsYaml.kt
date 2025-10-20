package no.vaccsca.amandman.model.data.dto

data class AmanDmanSettingsYaml(
    val timelines: Map<String, List<TimelineYaml>>, // ICAO -> list of timelines
    val connectionConfig: ConnectionConfigYaml
)

data class TimelineYaml(
    val title: String,
    val left: SideYaml? = null,   // nullable
    val right: SideYaml            // required
)

data class SideYaml(
    val runways: List<String>,
)

data class ConnectionConfigYaml(
    val atcClient: AtcClientYaml,
    val api: ApiYaml
)

data class AtcClientYaml(
    val host: String,
    val port: Int
)

data class ApiYaml(
    val host: String
)
