package no.vaccsca.amandman.model.data.repository

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.vaccsca.amandman.model.data.dto.AirportJson
import no.vaccsca.amandman.model.domain.valueobjects.Airport
import no.vaccsca.amandman.model.domain.valueobjects.LatLng
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo
import no.vaccsca.amandman.model.domain.valueobjects.Star
import no.vaccsca.amandman.model.domain.valueobjects.StarFix
import java.io.File
import java.io.FileNotFoundException

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

class NavdataRepository {

    private val yamlMapper = YAMLMapper().apply {
        registerKotlinModule()
    }

    val airports: List<Airport>

    init {
        val airportJsons = SettingsRepository.getAirportData().airports
        airports = airportJsons.map { (icao, vl) -> vl.toAirport(icao) }
    }

    private fun readYamlFile(filePath: String): StarYamlFile {
        val localFile = File(filePath)
        if (!localFile.exists()) {
            throw FileNotFoundException("YAML file not found: $filePath")
        }
        return yamlMapper.readValue(localFile)
    }

    fun AirportJson.toAirport(icao: String): Airport {
        val runways =  this.runwayThresholds.map { (rwId, rwData) ->
            RunwayInfo(
                id = rwId,
                latLng = LatLng(
                    lat = rwData.location.latitude,
                    lon = rwData.location.longitude,
                ),
                elevation = rwData.elevation,
                trueHeading = rwData.trueHeading
            )
        }

        val starsYaml = readYamlFile("config/stars/$icao.yaml")

        return Airport(
            icao = icao,
            location = LatLng(
                lat = this.location.latitude,
                lon = this.location.longitude,
            ),
            runways = runways,
            stars = parseStars(runways, starsYaml)
        )
    }

    fun parseStars(runways: List<RunwayInfo>, yaml: StarYamlFile): List<Star> {
        return yaml.STARS.map { starEntry ->
            val runwayInfo = runways.find { it.id == starEntry.runway }
                ?: throw IllegalArgumentException("Runway ${starEntry.runway} not found")

            val fixes = starEntry.waypoints.map { wp ->
                StarFix.StarFixBuilder(wp.id)
                    .apply {
                        wp.altitude?.let { altitude(it) }
                        wp.speed?.let { speed(it) }
                    }
                    .build()
            }

            Star(
                id = starEntry.name,
                airport = "N/A", // you can remove if unused
                runway = runwayInfo,
                fixes = fixes
            )
        }
    }
}
