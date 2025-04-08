package org.example

import org.example.entities.navigation.star.Constraint
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
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val headerRegex = Regex("""STAR\s+(\S+)\s+@(\d+)\s+ICAO:(\S+)\s+RWY:(\S+)""")
        val fixLineRegex = Regex("""(\S+):\s*(.+)""")
        val constraintRegex = Regex("""(alt|spd)([<>=]+)(\d+)""")

        val stars = mutableListOf<Star>()
        var currentId: String? = null
        var currentElevation: Int? = null
        var currentFixes = mutableListOf<StarFix>()

        fun flushCurrentStar() {
            if (currentId != null && currentElevation != null) {
                stars.add(Star(currentId!!, currentElevation!!, currentFixes))
            }
            currentId = null
            currentElevation = null
            currentFixes = mutableListOf()
        }

        for (line in lines) {
            if (line.startsWith("STAR")) {
                flushCurrentStar()

                val (id, elevationStr, _, _) = headerRegex.matchEntire(line)
                    ?.destructured
                    ?: throw IllegalArgumentException("Invalid STAR header: $line")

                currentId = id
                currentElevation = elevationStr.toInt()
            } else {
                val (fixId, constraintStr) = fixLineRegex.matchEntire(line)?.destructured
                    ?: throw IllegalArgumentException("Invalid fix line: $line")

                val constraints = constraintStr.split(",").map { it.trim() }

                val builder = StarFix.StarFixBuilder(fixId)

                for (constraint in constraints) {
                    val (type, op, valueStr) = constraintRegex.matchEntire(constraint)?.destructured
                        ?: throw IllegalArgumentException("Invalid constraint: $constraint")
                    val value = valueStr.toInt()

                    val parsed = when (op) {
                        "=" -> Constraint.Exact(value)
                        "<=" -> Constraint.Max(value)
                        ">=" -> Constraint.Min(value)
                        else -> throw IllegalArgumentException("Unsupported operator: $op")
                    }

                    when (type) {
                        "alt" -> builder.altitude(parsed)
                        "spd" -> builder.speed(parsed)
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