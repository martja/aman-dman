package no.vaccsca.amandman.model.data.repository

import no.vaccsca.amandman.model.data.dto.AirportJson
import no.vaccsca.amandman.model.data.dto.RunwayThresholdJson
import no.vaccsca.amandman.model.domain.valueobjects.Airport
import no.vaccsca.amandman.model.domain.valueobjects.RunwayInfo
import no.vaccsca.amandman.model.domain.valueobjects.LatLng
import no.vaccsca.amandman.model.domain.valueobjects.Star
import no.vaccsca.amandman.model.domain.valueobjects.StarFix
import java.io.File
import java.io.FileNotFoundException

class NavdataRepository {

    val airports: List<Airport>

    init {
        val airportJsons = SettingsRepository.getAirportData().airports
        airports = airportJsons.map { (icao, vl) -> vl.toAirport(icao) }
    }

    fun readTextFile(filePath: String): String {
        // Try reading from local file system (for development)
        val localFile = File(filePath)
        if (localFile.exists()) {
            return localFile.readText()
        } else {
            throw FileNotFoundException("Resource file not found: $filePath")
        }
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

        return Airport(
            icao = icao,
            location = LatLng(
                lat = this.location.latitude,
                lon = this.location.longitude,
            ),
            runways = runways,
            stars = parseStars(runways, readTextFile("config/stars.txt"))
        )
    }

    fun parseStars(runways: List<RunwayInfo>, input: String): List<Star> {
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("[") }

        val starHeaderRegex = Regex("STAR:(\\S+):(\\S+):(\\S+)")
        val constraintRegex = Regex("""(A|S)=(\d+)""")

        val stars = mutableListOf<Star>()
        var currentId: String? = null
        var currentAirport: String? = null
        var currentRunway: String? = null
        var currentFixes = mutableListOf<StarFix>()

        fun flushCurrentStar() {
            if (currentId != null && currentAirport != null && currentRunway != null) {
                val rwy = runways.find { it.id == currentRunway }
                    ?: throw IllegalArgumentException("Runway $currentRunway not found for airport $currentAirport")

                stars.add(Star(currentId!!, currentAirport!!, rwy, currentFixes))
            }
            currentId = null
            currentAirport = null
            currentRunway = null
            currentFixes = mutableListOf()
        }

        for (line in lines) {
            if (line.startsWith("STAR:")) {
                flushCurrentStar()

                val (icao, rwy, id) = starHeaderRegex.matchEntire(line)?.destructured
                    ?: throw IllegalArgumentException("Invalid STAR header: $line")
                currentId = id
                currentAirport = icao
                currentRunway = rwy
            } else {
                val parts = line.split(":").map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.isEmpty()) continue

                val fixId = parts[0]
                val builder = StarFix.StarFixBuilder(fixId)

                val constraintParts = parts.drop(1)
                for (constraint in constraintParts) {
                    val (type, valueStr) = constraintRegex.matchEntire(constraint)?.destructured
                        ?: throw IllegalArgumentException("Invalid constraint: $constraint")
                    val value = valueStr.toInt()

                    when (type) {
                        "A" -> builder.altitude(value)
                        "S" -> builder.speed(value)
                        else -> throw IllegalArgumentException("Unknown constraint type: $type")
                    }
                }

                currentFixes.add(builder.build())
            }
        }

        flushCurrentStar()
        return stars
    }
}