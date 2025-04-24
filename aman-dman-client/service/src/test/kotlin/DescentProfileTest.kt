import kotlinx.datetime.Clock
import org.example.*
import org.example.DescentProfileService.generateDescentSegments
import org.example.config.AircraftPerformanceData
import org.example.entities.navigation.AircraftPosition
import org.example.entities.navigation.RoutePoint
import org.example.entities.navigation.star.Star
import org.example.entities.navigation.star.StarFix
import org.example.util.NavigationUtils.dmsToDecimal
import org.example.util.NavigationUtils.interpolatePositionAlongPath
import org.example.util.PhysicsUtils
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

fun starFix(id: String, block: StarFix.StarFixBuilder.() -> Unit): StarFix {
    return StarFix.StarFixBuilder(id).apply(block).build()
}

val lunip4l = Star(
    id = "LUNIP4L",
    airport = "ENGM",
    runway = "01L",
    fixes = listOf(
        starFix("LUNIP") {
            speed(250)
        },
        starFix("GM416") {
            altitude(11000)
            speed(220)
        },
        starFix("INSUV") {
            altitude(5000)
            speed(200)
        },
        starFix("NOSLA") {
            altitude(4000)
            speed(180)
        },
        starFix("XEMEN") {
            altitude(3500)
            speed(170)
        },
        starFix("ENGM") {
            altitude(700)
        }
    )
)

class DescentProfileTest {
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

    /*@Test
    fun `calculate ETA`() {
        val descentSegments = calculateTestDescent(testRoute)
        val remainingTime = descentSegments.first().remainingTime

        // To wkt linestring
        val wkt = "LINESTRING (" + descentSegments.joinToString(",") { it.position.lon.toString() + " " + it.position.lat.toString() } + ")"

        assertEquals(20.minutes + 46.seconds, remainingTime)
    }*/

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
    fun `When aircraft is heading away from the first waypoint, the turn radius will be taken into account`() {
        val descentSegmentsOriginalRoute = calculateTestDescent(testRoute)

        val testRouteWithInitialHeading = testRoute.mapIndexed { index, routePoint ->
            if (index == 0) {
                routePoint.copy(
                    position = routePoint.position.interpolatePositionAlongPath(
                        LatLng(60.0, 11.0),
                        0.5
                    )
                )
            } else {
                routePoint
            }
        }
    }

    @Test
    fun `Removing a fix that causes a shorter flight path should reduce time`() {
        val descentSegmentsOriginalRoute = calculateTestDescent(testRoute)
        val remainingTimeOriginalRoute = descentSegmentsOriginalRoute.first().remainingTime

        val testRouteB = testRoute.filter { it.id != "GM415" && it.id != "GM414" }
        val descentSegmentsModifiedRoute = calculateTestDescent(testRouteB)

        val remainingTimeModifiedRoute = descentSegmentsModifiedRoute.first().remainingTime

        assertTrue { remainingTimeOriginalRoute > remainingTimeModifiedRoute }
    }

    @Test
    fun `Descent path adheres to altitude restrictions`() {
        val descentSegments = calculateTestDescent(testRoute)
        val altitudeViolations = descentSegments.filter { descentSegment ->
            val passingWp = testRoute.find { wp -> wp.position == descentSegment.position }
            val altitudeConstraint = lunip4l.fixes.find { wp -> wp.id == passingWp?.id }?.typicalAltitude

            if (altitudeConstraint == null) {
                return@filter false
            }

            val wrongHeight = abs(descentSegment.altitude - altitudeConstraint) > 500

            if (wrongHeight) {
                println("Violation at ${passingWp!!.id} - ${descentSegment.altitude} ft. Constraint = ${altitudeConstraint}")
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
            PhysicsUtils.iasToTas(220, 20000, -20)
        )

        assertEquals(
            254,
            PhysicsUtils.iasToTas(220, 10000, -10)
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
        val windFromNorth = Wind(speedKts = 20, directionDeg = 360)
        val aircraftTas = 220

        val gsWithHeadwind = PhysicsUtils.tasToGs(
            tas = aircraftTas,
            wind = windFromNorth,
            track = 360
        )

        val gsWithTailwind = PhysicsUtils.tasToGs(
            tas = aircraftTas,
            wind = windFromNorth,
            track = 180
        )

        val gsWithCrosswind = PhysicsUtils.tasToGs(
            tas = aircraftTas,
            wind = windFromNorth,
            track = 90
        )


        assertEquals(200, gsWithHeadwind)
        assertEquals(240, gsWithTailwind)
        assertEquals(221, gsWithCrosswind)
    }

    private fun calculateTestDescent(remainingRoute: List<RoutePoint>): List<EstimatedProfilePoint> {
        val weatherData = listOf(
            WeatherLayer(0, 0, wind = Wind(180, 0)),
            WeatherLayer(10000, -10, wind = Wind(180, 10)),
            WeatherLayer(20000, -20, wind = Wind(180, 20)),
            WeatherLayer(30000, -30, wind = Wind(180, 30)),
        )

        val weatherProfile = VerticalWeatherProfile(
            Clock.System.now(),
            currentPosition.position,
            weatherData.toMutableList()
        )

        val aircraftPerformance = AircraftPerformanceData.get("B738")

        val descentSegments = remainingRoute.generateDescentSegments(
            currentPosition,
            weatherProfile,
            lunip4l,
            aircraftPerformance
        )

        println("Descent segments for route:")
        descentSegments.forEach { println(it) }

        return descentSegments
    }
}

fun List<RoutePoint>.getRouteDistance() =
    this.zipWithNext().sumOf { (from, to) ->
        from.position.distanceTo(to.position)
    }
