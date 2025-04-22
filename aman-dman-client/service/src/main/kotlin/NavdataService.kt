package org.example

import org.example.entities.navigation.star.Star
import org.example.entities.navigation.star.StarFix

class NavdataService {

    val stars: List<Star>

    init {
        stars = parseStars(readFileFromResources("stars.txt"))
    }

    fun readFileFromResources(fileName: String): String =
        this::class.java.classLoader.getResource(fileName)?.readText()
            ?: throw IllegalArgumentException("File not found: $fileName")

    fun parseStars(input: String): List<Star> {
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
                stars.add(Star(currentId!!, currentAirport!!, currentRunway!!, currentFixes))
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
