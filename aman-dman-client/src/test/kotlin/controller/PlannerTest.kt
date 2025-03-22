package controller

import org.example.*
import org.example.model.entities.WeatherData
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes


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
    }
)

val performanceData = mapOf(
    "A320" to AircraftPerformance(1800, 280, 250),
    "B738" to AircraftPerformance(1800, 280, 250),
)

class PlannerTest {



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
        val currentPosition = AircraftPosition(
            dmsToDecimal("""58°50'25.3"N  011°20'7.2"E"""),
            12000,
            250,
            180
        )

        val remainingRoute = listOf(
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

        val weatherData = listOf(
            WeatherData(0, 180, 0, 0),
            WeatherData(10000, 180, 10, -10),
            WeatherData(20000, 180, 20, -20),
            WeatherData(30000, 180, 30, -30),
        )

        val eta = remainingRoute.estimateRemainingTime(currentPosition, weatherData, lunip4l, performanceData["A320"]!!)

        assertEquals(1.minutes, eta)
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