package no.vaccsca.amandman.model.data.config.yaml

data class StarYamlFile(
    val STARS: List<StarYamlEntry>
)

data class StarYamlEntry(
    val runway: String,
    val name: String,
    val waypoints: List<StarWaypointYaml>
)

data class StarWaypointYaml(
    val id: String,
    val altitude: Int? = null,
    val speed: Int? = null
)