package org.example

import org.example.model.entities.WeatherData
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class StarFix(
    val id: String,
    val starAltitudeConstraint: StarAltitudeConstraint? = null,
    val starSpeedConstraint: StarSpeedConstraint? = null
)

data class StarAltitudeConstraint(
    val minFt: Int? = null,
    val maxFt: Int? = null,
    val exactFt: Int? = null,
)

data class StarSpeedConstraint(
    val minKts: Int? = null,
    val maxKts: Int? = null,
    val exactKts: Int? = null
)

data class AircraftPosition(
    val position: LatLng,
    val altitudeFt: Int,
    val groundspeedKts: Int,
    val trackDeg: Int
)

class StarFixBuilder(private val id: String) {
    private var minAltFt: Int? = null
    private var maxAltFt: Int? = null
    private var exactAltFt: Int? = null

    private var minSpeedKts: Int? = null
    private var maxSpeedKts: Int? = null
    private var exactSpeedKts: Int? = null

    fun minAlt(ft: Int) = apply { minAltFt = ft }
    fun maxAlt(ft: Int) = apply { maxAltFt = ft }
    fun exactAlt(ft: Int) = apply { exactAltFt = ft }

    fun minSpeed(kts: Int) = apply { minSpeedKts = kts }
    fun maxSpeed(kts: Int) = apply { maxSpeedKts = kts }
    fun exactSpeed(kts: Int) = apply { exactSpeedKts = kts }

    fun build(): StarFix {
        val altConstraint = if (minAltFt != null || maxAltFt != null || exactAltFt != null) {
            StarAltitudeConstraint(minAltFt, maxAltFt, exactAltFt)
        } else null

        val starSpeedConstraint = if (minSpeedKts != null || maxSpeedKts != null || exactSpeedKts != null) {
            StarSpeedConstraint(minSpeedKts, maxSpeedKts, exactSpeedKts)
        } else null

        return StarFix(id, altConstraint, starSpeedConstraint)
    }
}

fun starFix(id: String, block: StarFixBuilder.() -> Unit): StarFix {
    return StarFixBuilder(id).apply(block).build()
}

data class RouteLeg(
    val from: StarFix,
    val to: StarFix,
    val isVector: Boolean = false
)

data class Route(
    val legs: List<RouteLeg>
)

data class DescentSegment(
    val leg: RouteLeg,
    val fromAltFt: Int,
    val toAltFt: Int,
    val groundspeedKts: Int,
    val descentRateFpm: Int
)

data class RoutePoint(
    val id: String,
    val position: LatLng
)

data class LatLng(
    val lat: Double,
    val lon: Double
)

fun LatLng.distanceTo(latLng: LatLng): Double {
    val lat1 = Math.toRadians(this.lat)
    val lon1 = Math.toRadians(this.lon)
    val lat2 = Math.toRadians(latLng.lat)
    val lon2 = Math.toRadians(latLng.lon)

    val dlon = lon2 - lon1
    val dlat = lat2 - lat1

    val a = Math.pow(Math.sin(dlat / 2), 2.0) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2.0)
    val c = 2 * Math.asin(Math.sqrt(a))
    val radius = 3440.065 // in nautical miles
    return radius * c
}

fun LatLng.bearingTo(latLng: LatLng): Int {
    val lat1 = Math.toRadians(this.lat)
    val lon1 = Math.toRadians(this.lon)
    val lat2 = Math.toRadians(latLng.lat)
    val lon2 = Math.toRadians(latLng.lon)

    val dlon = lon2 - lon1

    val y = Math.sin(dlon) * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dlon)

    val bearing = Math.toDegrees(Math.atan2(y, x))

    return ((bearing + 360) % 360).roundToInt()
}

fun List<RoutePoint>.getRouteDistance() =
    this.zipWithNext().sumOf { (from, to) ->
        from.position.distanceTo(to.position)
    }


fun List<RoutePoint>.estimateRemainingTime(
    target: AircraftPosition,
    windData: List<WeatherData>,
    star: List<StarFix>,
    aircraftPerformance: AircraftPerformance
): Duration {
    var duration = 0.seconds
    var currentPosition = target.position
    var currentAltitude = target.altitudeFt
    val airfieldAltitude = 700

    // Pre-map STAR constraints by ID for fast lookup
    val starMap = star.associateBy { it.id }

    var currentSpeedConstraint: Int? = null

    for (i in indices) {
        val targetWaypoint = this[i]
        val remainingRoute = this.subList(i, this.size)

        val nextAltitudeConstraint = remainingRoute.firstNotNullOfOrNull { starMap[it.id]?.starAltitudeConstraint?.exactFt }
        val targetAltitude = nextAltitudeConstraint ?: airfieldAltitude

        val routeUntilConstraint = remainingRoute.takeWhile { starMap[it.id]?.starAltitudeConstraint?.exactFt == null }

        // Descent phase
        val descentPath = aircraftPerformance.computeDescentPath(
            fromAltFt = currentAltitude,
            toAltFt = targetAltitude,
            from = currentPosition,
            to = targetWaypoint.position,
            weatherData = windData,
            distanceToNextAltConstraint = routeUntilConstraint.getRouteDistance(),
            maxIas = currentSpeedConstraint
        )

        descentPath.forEach { step ->
            duration += step.time
            currentAltitude = step.altitudeFt
            currentPosition = step.position

            println("${step.length.format(2)} NM at GS=${step.groundSpeed} kts, TAS=${step.tas} kts = ${step.time}, to: ${step.altitudeFt}")
        }

        // Snap to waypoint at end of segment
        currentPosition = targetWaypoint.position
        currentSpeedConstraint = starMap[targetWaypoint.id]?.starSpeedConstraint?.maxKts ?: currentSpeedConstraint
        println("Reached ${targetWaypoint.id}")
    }

    println("Estimated duration: $duration")
    return duration
}


/* *************** */

data class DescentStep(
    val position: LatLng,
    val altitudeFt: Int,
    val length: Float,
    val time: Duration,
    val groundSpeed: Int,
    val tas: Int
)

fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

fun AircraftPerformance.computeDescentPath(
    fromAltFt: Int,
    toAltFt: Int,
    from: LatLng,
    to: LatLng,
    weatherData: List<WeatherData>,
    distanceToNextAltConstraint: Double,
    maxIas: Int?
): List<DescentStep> {
    val descentPath = mutableListOf<DescentStep>()

    // Estimate average descent rate and groundspeed for planning
    val avgAltitude = (fromAltFt + toAltFt) / 2
    val avgWeatherData = weatherData.interpolateWeatherAtAltitude(avgAltitude)
    val avgDescentRateFpm = this.estimateDescentRate(avgAltitude)
    val avgGroundSpeedKts = iasToTas(this.initialDescentSpeedIas, avgAltitude, avgWeatherData.temperatureC)

    // Calculate minimum distance needed to descend
    val totalDescentFt = fromAltFt - toAltFt
    val totalDescentMinutes = totalDescentFt / avgDescentRateFpm.toDouble()
    val requiredDescentDistanceNm = avgGroundSpeedKts * (totalDescentMinutes / 60.0)

    val bearingToNext = from.bearingTo(to)
    var distToNext = from.distanceTo(to)
    var currentAltitude = fromAltFt
    var currentPosition = from

    val deltaTime = 10.0 // seconds

    while (distToNext > 0 && distToNext > distanceToNextAltConstraint && currentAltitude > toAltFt) {
        val distanceToGo = currentPosition.distanceTo(to)

        val stepWeather = weatherData.interpolateWeatherAtAltitude(currentAltitude)

        val expectedIas = maxIas
            ?: if (currentAltitude > 10_000) this.initialDescentSpeedIas else this.below10kSpeedIas

        val stepTas = iasToTas(expectedIas, currentAltitude, stepWeather.temperatureC)
        val stepGroundspeedKts = tasToGs(stepTas, stepWeather, bearingToNext)
        val stepDistanceNm = (stepGroundspeedKts * deltaTime) / 3600.0

        if (distanceToGo <= requiredDescentDistanceNm) {
            // Begin descent
            val descentRateFpm = this.estimateDescentRate(currentAltitude)
            val verticalSpeed = descentRateFpm / 60.0 // ft/sec

            val stepDescentFt = verticalSpeed * deltaTime
            val nextAlt = max(currentAltitude - stepDescentFt.roundToInt(), toAltFt)
            val nextPos = currentPosition.interpolatePositionAlongPath(to, stepDistanceNm)

            descentPath.add(
                DescentStep(
                    position = nextPos,
                    altitudeFt = nextAlt,
                    length = stepDistanceNm.toFloat(),
                    time = deltaTime.seconds,
                    groundSpeed = stepGroundspeedKts,
                    tas = stepTas
                )
            )

            currentAltitude = nextAlt
            currentPosition = nextPos
            distToNext -= stepDistanceNm
        } else {
            val nextPos = currentPosition.interpolatePositionAlongPath(to, stepDistanceNm)

            descentPath.add(
                DescentStep(
                    position = nextPos,
                    altitudeFt = currentAltitude,
                    length = stepDistanceNm.toFloat(),
                    time = deltaTime.seconds,
                    groundSpeed = stepGroundspeedKts,
                    tas = stepTas
                )
            )

            currentPosition = nextPos
            distToNext -= stepDistanceNm
        }
    }

    return descentPath
}

fun iasToTas(ias: Int, altitudeFt: Int, tempCelsius: Int): Int {
    val tempKelvin = tempCelsius + 273.15
    val altitudeMeters = altitudeFt * 0.3048

    // Constants
    val P0 = 101325.0 // Sea level pressure in Pascals
    val T0 = 288.15   // Sea level temperature in Kelvin
    val L = 0.0065    // Temperature lapse rate (K/m)
    val R = 287.05    // Specific gas constant for dry air (J/kg·K)
    val g = 9.80665   // Gravity (m/s²)

    // ISA Pressure at altitude
    val pressure = P0 * Math.pow(1 - (L * altitudeMeters) / T0, g / (R * L))

    // Air density at altitude
    val rho = pressure / (R * tempKelvin)
    val rho0 = P0 / (R * T0)

    // TAS estimation
    return (ias * Math.sqrt(rho0 / rho)).roundToInt()
}

fun tasToGs(tas: Int, weatherData: WeatherData, track: Int): Int {
    val windAngleRad = Math.toRadians((weatherData.windDirectionDeg - track).toDouble())
    val windComponent = weatherData.windSpeedKts * Math.cos(windAngleRad)
    return (tas + windComponent).roundToInt()
}

fun List<WeatherData>.interpolateWeatherAtAltitude(altitudeFt: Int): WeatherData {
    // Interpolate wind data based on the two closest altitudes
    val sorted = this.sortedBy { it.flightLevelFt }
    val lower = sorted.last { it.flightLevelFt <= altitudeFt }
    val upper = sorted.first { it.flightLevelFt > altitudeFt }

    val ratio = (altitudeFt - lower.flightLevelFt).toFloat() / (upper.flightLevelFt - lower.flightLevelFt).toFloat()

    val direction = (1 - ratio) * lower.windDirectionDeg + ratio * upper.windDirectionDeg
    val speed = (1 - ratio) * lower.windSpeedKts + ratio * upper.windSpeedKts
    val temperature = (1 - ratio) * lower.temperatureC + ratio * upper.temperatureC

    return WeatherData(
        flightLevelFt = altitudeFt,
        windDirectionDeg = direction.roundToInt(),
        windSpeedKts = speed.roundToInt(),
        temperatureC = temperature.roundToInt()
    )
}

data class AircraftPerformance(
    val idleDescentRateFpm: Int,
    val initialDescentSpeedIas: Int,
    val below10kSpeedIas: Int,
    val landingSpeed: Int
)

fun AircraftPerformance.estimateDescentRate(altitudeFt: Int): Int {
    return this.idleDescentRateFpm
}

fun LatLng.interpolatePositionAlongPath(endPoint: LatLng, distanceNm: Double): LatLng {
    val radiusNm = 3440.065 // Earth radius in NM

    val lat1 = Math.toRadians(this.lat)
    val lon1 = Math.toRadians(this.lon)
    val lat2 = Math.toRadians(endPoint.lat)
    val lon2 = Math.toRadians(endPoint.lon)

    val delta = centralAngle(lat1, lon1, lat2, lon2)

    if (delta == 0.0) return this

    val fraction = distanceNm / (radiusNm * delta)

    val A = sin((1 - fraction) * delta) / sin(delta)
    val B = sin(fraction * delta) / sin(delta)

    val x = A * cos(lat1) * cos(lon1) + B * cos(lat2) * cos(lon2)
    val y = A * cos(lat1) * sin(lon1) + B * cos(lat2) * sin(lon2)
    val z = A * sin(lat1) + B * sin(lat2)

    val lat = atan2(z, sqrt(x * x + y * y))
    val lon = atan2(y, x)

    return LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
}

fun centralAngle(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return acos(
        sin(lat1) * sin(lat2) +
                cos(lat1) * cos(lat2) * cos(lon2 - lon1)
    )
}


/**
 * get_aircraft_performance(type, weight, altitude, speed, config) → performance_profile
 * interpolate_wind(lat, lon, alt) → headwind_component
 * interpolate_temperature(lat, lon, alt) → temperature
 * calculate_true_airspeed(performance_profile, altitude, temperature) → tas
 * calculate_groundspeed(tas, headwind_component) → gs
 * calculate_distance(pointA, pointB) → dist
 */

