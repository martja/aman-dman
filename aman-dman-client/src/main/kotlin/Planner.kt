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
    val maxKts: Int? = null,
    val minKts: Int? = null,
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

    return Math.toDegrees(Math.atan2(y, x)).roundToInt()
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
            distanceToNextAltConstraint = routeUntilConstraint.getRouteDistance()
        )

        descentPath.forEach { step ->
            duration += step.time
            currentAltitude = step.altitudeFt
            currentPosition = step.position

            println("${step.length.format(2)} NM at ${step.groundSpeed} kts, TAS ${step.tas} = ${step.time}, to: ${step.altitudeFt}")
        }

        // Snap to waypoint at end of segment
        currentPosition = targetWaypoint.position
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
    distanceToNextAltConstraint: Double
): List<DescentStep> {
    val descentPath = mutableListOf<DescentStep>()


    // Estimate average descent rate and groundspeed for planning
    val avgAltitude = (fromAltFt + toAltFt) / 2
    val avgWeatherData = weatherData.interpolateWeatherAtAltitude(avgAltitude)
    val avgDescentRateFpm = this.estimateDescentRate(avgAltitude)
    val avgGroundSpeedKts = this.estimateGroundSpeed(avgAltitude, from.bearingTo(to), avgWeatherData)

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
        val stepGroundspeedKts = this.estimateGroundSpeed(currentAltitude, bearingToNext, stepWeather)
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
                    tas = this.calculateTrueAirspeed(currentAltitude, stepWeather.temperatureC),
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
                    tas = this.calculateTrueAirspeed(currentAltitude, stepWeather.temperatureC),
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
    val pressureAltitude = 145366 * (1 - Math.pow((tempKelvin / 288.15), 0.235))
    val pressureRatio = Math.pow((1 - 0.0000068756 * pressureAltitude), 5.2559)
    return (ias * Math.sqrt(288.15 / tempKelvin) * pressureRatio).toInt()
}

fun tasToGs(tas: Int, windSpeed: Int, windDirection: Int, track: Int): Int {
    val windAngleRad = Math.toRadians((windDirection - track).toDouble())
    val windComponent = windSpeed * Math.cos(windAngleRad)
    return (tas + windComponent).roundToInt()
}

fun List<WeatherData>.interpolateWeatherAtAltitude(altitudeFt: Int): WeatherData {
    // Interpolate wind data based on the two closest altitudes
    val sorted = this.sortedBy { it.flightLevelFt }
    val lower = sorted.last { it.flightLevelFt <= altitudeFt }
    val upper = sorted.first { it.flightLevelFt > altitudeFt }

    val ratio = (altitudeFt - lower.flightLevelFt).toFloat() / (upper.flightLevelFt - lower.flightLevelFt).toFloat()

    val direction = (1 - ratio) * lower.windDirectionKts + ratio * upper.windDirectionKts
    val speed = (1 - ratio) * lower.windSpeedKts + ratio * upper.windSpeedKts
    val temperature = (1 - ratio) * lower.temperatureC + ratio * upper.temperatureC

    return WeatherData(
        flightLevelFt = altitudeFt,
        windDirectionKts = direction.roundToInt(),
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

fun AircraftPerformance.calculateTrueAirspeed(altitudeFt: Int, tempCelsius: Int): Int {
    if (altitudeFt > 10_000)
        return iasToTas(this.initialDescentSpeedIas, altitudeFt, tempCelsius)
    else
        return iasToTas(this.below10kSpeedIas, altitudeFt, tempCelsius)
}

fun AircraftPerformance.estimateDescentRate(altitudeFt: Int): Int {
    return this.idleDescentRateFpm
}

fun AircraftPerformance.estimateGroundSpeed(altitudeFt: Int, trackDeg: Int, weatherData: WeatherData?): Int {
    val tas = this.calculateTrueAirspeed(altitudeFt, weatherData?.temperatureC ?: 0)
    return tasToGs(
        tas = tas,
        windSpeed = weatherData?.windSpeedKts ?: 0,
        windDirection = weatherData?.windDirectionKts ?: 0,
        track = trackDeg
    )
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

