package controller

import org.example.*
import org.example.config.AircraftPerformanceData
import org.example.model.entities.WeatherData
import org.example.model.entities.WindData
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


val lunip4l = listOf(
    starFix("LUNIP") {
        maxSpeed(250)
    },
    starFix("DEVKU") {
        minAlt(12000)
        maxSpeed(250)
    },
    starFix("GM416") {
        exactAlt(11000)
        maxSpeed(220)
    },
    starFix("GM417") {
        exactAlt(11000)
        maxSpeed(220)
    },
    starFix("GM415") {
        exactAlt(11000)
        maxSpeed(220)
    },
    starFix("GM414") {
        exactAlt(11000)
        maxSpeed(220)
    },
    starFix("INSUV") {
        minAlt(5000)
        exactAlt(5000)
        maxSpeed(220)
    },
    starFix("NOSLA") {
        maxSpeed(200)
    },
    starFix("XEMEN") {
        exactAlt(3500)
        maxSpeed(200)
    }
)

class PlannerTest {
    val currentPosition = AircraftPosition(
        dmsToDecimal("""58°50'25.3"N  011°20'7.2"E"""),
        22000,
        250,
        180
    )

    val testRoute = listOf(
        RoutePoint("CURRENT", currentPosition.position),
        RoutePoint("LUNIP", dmsToDecimal("""59°10'60.0"N  011°18'55.0"E""")),
        RoutePoint("DEVKU", dmsToDecimal("""59°27'7.9"N  011°15'34.4"E""")),
        RoutePoint("GM416", dmsToDecimal("""59°37'49.7"N  011°13'1.2"E""")),
        RoutePoint("GM417", dmsToDecimal("""59°39'55.7"N  011°24'37.9"E""")),
        RoutePoint("GM415", dmsToDecimal("""59°43'57.3"N  011°34'9.0"E""")),
        RoutePoint("GM414", dmsToDecimal("""59°49'18.7"N  011°40'29.1"E""")),
        RoutePoint("INSUV", dmsToDecimal("""59°55'32.2"N  011°6'50.6"E""")),
        RoutePoint("NOSLA", dmsToDecimal("""59°59'1.2"N  010°59'51.2"E""")),
        RoutePoint("XEMEN", dmsToDecimal("""60°2'10.4"N  011°1'39.4"E""")),
        RoutePoint("ONE", dmsToDecimal("""60°10'40.6"N  011°6'41.0"E""")),
    )

    @Test
    fun `STAR length matches AIRAC spec`() {
        val lunip4lRoute = listOf(
            RoutePoint("LUNIP", dmsToDecimal("""59°10'60.0"N  011°18'55.0"E""")),
            RoutePoint("DEVKU", dmsToDecimal("""59°27'7.9"N  011°15'34.4"E""")),
            RoutePoint("GM416", dmsToDecimal("""59°37'49.7"N  011°13'1.2"E""")),
            RoutePoint("GM417", dmsToDecimal("""59°39'55.7"N  011°24'37.9"E""")),
            RoutePoint("GM415", dmsToDecimal("""59°43'57.3"N  011°34'9.0"E""")),
            RoutePoint("GM414", dmsToDecimal("""59°49'18.7"N  011°40'29.1"E""")),
            RoutePoint("INSUV", dmsToDecimal("""59°55'32.2"N  011°6'50.6"E""")),
        )

        assertEquals(64, lunip4lRoute.getRouteDistance().roundToInt())
    }

    @Test
    fun `calculate ETA`() {
        val descentSegments = calculateTestDescent(testRoute)
        val remainingTime = descentSegments.first().remainingTime

        // To wkt linestring
        val wkt = "LINESTRING (" + descentSegments.joinToString(",") { it.position.lon.toString() + " " + it.position.lat.toString() } + ")"

        assertEquals(20.minutes + 46.seconds, remainingTime)
    }

    @Test
    fun `Removing waypoints along a straight line should not affect ETA`() {
        val descentSegmentsOriginalRoute = calculateTestDescent(testRoute)
        val remainingTimeOriginalRoute = descentSegmentsOriginalRoute.first().remainingTime

        val testRouteB = testRoute.filter { it.id != "DEVKU" }
        val descentSegmentsModifiedRoute = calculateTestDescent(testRouteB)

        val remainingTimeModifiedRoute = descentSegmentsModifiedRoute.first().remainingTime

        // Allow 2 seconds difference
        assertTrue(remainingTimeOriginalRoute - remainingTimeModifiedRoute < 2.0.seconds)
    }

    @Test
    fun `Removing that causes a shorter flight path should reduce time`() {
        val descentSegmentsOriginalRoute = calculateTestDescent(testRoute)
        val remainingTimeOriginalRoute = descentSegmentsOriginalRoute.first().remainingTime

        val testRouteB = testRoute.filter { it.id != "GM415" && it.id != "GM414" }
        val descentSegmentsModifiedRoute = calculateTestDescent(testRouteB)

        val remainingTimeModifiedRoute = descentSegmentsModifiedRoute.first().remainingTime

        assertEquals(remainingTimeOriginalRoute, 20.minutes + 46.seconds)
        assertEquals(remainingTimeModifiedRoute, 18.minutes + 29.seconds)
    }

    @Test
    fun `Descent path adheres to altitude restrictions`() {
        val descentSegments = calculateTestDescent(testRoute)
        val altitudeViolations = descentSegments.filter { descentSegment ->
            val passingWp = testRoute.find { wp -> wp.position == descentSegment.position }
            val altitudeConstraint = lunip4l.find { wp -> wp.id == passingWp?.id }?.starAltitudeConstraint

            if (altitudeConstraint == null) {
                return@filter false
            }

            val tooLow = altitudeConstraint.minFt != null && descentSegment.targetAltitude < altitudeConstraint.minFt!!
            val tooHigh = altitudeConstraint.maxFt != null && descentSegment.targetAltitude > altitudeConstraint.maxFt!!

            if (tooLow || tooHigh) {
                println("Violation at ${descentSegment.position} - ${descentSegment.targetAltitude} ft")
                return@filter true
            }
            return@filter false
        }

        assertEquals(emptyList(), altitudeViolations)
    }

    @Test
    fun `The length of the descent segments route should be equal to the original route`() {
        val descentSegments = calculateTestDescent(testRoute)
        val descentSegmentsLength = descentSegments.zipWithNext { a, b -> a.position.distanceTo(b.position) }.sum()

        val originalRouteLength = testRoute.getRouteDistance()

        // Allow 0.05% difference
        val maxDifference = originalRouteLength * 0.0005
        assertEquals(originalRouteLength, descentSegmentsLength, maxDifference)
    }

    @Test
    fun `Descent segments should not contain duplicates`() {
        val descentSegments = calculateTestDescent(testRoute)
        val nDistinctPoints = descentSegments.map { it.position.lat to it.position.lon }.distinct().size
        val latLngDuplicates = nDistinctPoints - descentSegments.size

        assertEquals(0, latLngDuplicates)
    }

    @Test
    fun `Descent segments should include all waypoints`() {
        val descentSegments = calculateTestDescent(testRoute)
        val waypointsNotInDescentSegments = testRoute
            .filter { waypoint -> descentSegments.none { it.position == waypoint.position } }
            .map { it.id }

        assertEquals(emptyList(), waypointsNotInDescentSegments)
    }

    @Test
    fun `Calculates bearing correctly`() {
        LatLng(60.0, 0.0).bearingTo(LatLng(60.0, 1.0)).let {
            assertEquals(90, it)
        }

        LatLng(60.0, 0.0).bearingTo(LatLng(61.0, 0.0)).let {
            assertEquals(0, it)
        }

        LatLng(60.0, 0.0).bearingTo(LatLng(60.0, -1.0)).let {
            assertEquals(270, it)
        }

        LatLng(60.0, 0.0).bearingTo(LatLng(59.0, 0.0)).let {
            assertEquals(180, it)
        }
    }

    @Test
    fun `Calculates IAS to TAS correctly`() {
        assertEquals(
            304,
            iasToTas(220, 20000, -20)
        )

        assertEquals(
            254,
            iasToTas(220, 10000, -10)
        )
    }

    @Test
    fun `Interpolate distance along path`() {
        val origin = LatLng(60.0, 11.0)
        val newPoint = origin.interpolatePositionAlongPath(LatLng(60.0, 12.0), 0.5)
        val tolerance = 0.0001

        assertEquals(60.0, newPoint.lat, tolerance)
        assertTrue(newPoint.lon > 11.0)
        assertTrue(newPoint.lon < 12.0)
        assertEquals(0.5, origin.distanceTo(newPoint), tolerance)
    }

    @Test
    fun `Wind direction should affect ground speed`() {
        val windFromNorth = WindData(speedKts = 20, directionDeg = 360)
        val aircraftTas = 220

        val gsWithHeadwind = tasToGs(
            tas = aircraftTas,
            wind = windFromNorth,
            track = 360
        )

        val gsWithTailwind = tasToGs(
            tas = aircraftTas,
            wind = windFromNorth,
            track = 180
        )

        val gsWithCrosswind = tasToGs(
            tas = aircraftTas,
            wind = windFromNorth,
            track = 90
        )


        assertEquals(200, gsWithHeadwind)
        assertEquals(240, gsWithTailwind)
        assertEquals(220, gsWithCrosswind)
    }

    fun calculateTestDescent(remainingRoute: List<RoutePoint>): List<DescentSegment> {
        val weatherData = listOf(
            WeatherData(0, 0, wind = WindData(180, 0)),
            WeatherData(10000, -10, wind = WindData(180, 10)),
            WeatherData(20000, -20, wind = WindData(180, 20)),
            WeatherData(30000, -30, wind = WindData(180, 30)),
        )

        val aircraftPerformance = AircraftPerformanceData.get("B738")

        val descentSegments = remainingRoute.generateDescentSegments(
            currentPosition,
            weatherData,
            lunip4l,
            aircraftPerformance
        )

        println("Descent segments for route:")
        descentSegments.forEach { println(it) }

        return descentSegments
    }
}


fun dmsToDecimal(dms: String): LatLng {
    val regex = Regex("""(\d+)°(\d+)'(\d+(?:\.\d+)?)"([NSEW])""")
    val matches = regex.findAll(dms)

    val coords = matches.map { match ->
        val (deg, min, sec, dir) = match.destructured
        val decimal = deg.toDouble() + min.toDouble() / 60 + sec.toDouble() / 3600
        when (dir) {
            "S", "W" -> -decimal
            else -> decimal
        }
    }.toList()

    if (coords.size != 2) {
        throw IllegalArgumentException("Invalid coordinate format: $dms")
    }

    return LatLng(coords[0], coords[1]) // (latitude, longitude)
}

