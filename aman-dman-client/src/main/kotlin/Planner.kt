package org.example

import org.example.model.entities.WeatherData
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

class Planner {
    /*fun Route.generateDescentSegments(initialAlt: Int, initialGs: Int): List<DescentSegment> {
        val segments = mutableListOf<DescentSegment>()
        var currentAlt = initialAlt
        var currentGs = initialGs

        for (leg in this.legs) {
            val toAlt = leg.to.starAltitudeConstraint?.exactFt
                ?: currentAlt // if no constraint, maintain level

            if (toAlt < currentAlt) {
                val distanceNm = computeLegDistance(leg)
                val descentRateFpm = estimateDescentRate(currentAlt, toAlt, distanceNm)

                segments.add(
                    DescentSegment(
                        leg = leg,
                        fromAltFt = currentAlt,
                        toAltFt = toAlt,
                        groundspeedKts = currentGs,
                        descentRateFpm = descentRateFpm
                    )
                )

                currentAlt = toAlt
            } else {
                // Level leg, maybe speed change only
                currentAlt = toAlt
            }

            // TODO: Adjust GS if speed constraint exists
        }

        return segments
    }*/

    fun estimateDescentRate(fromAltFt: Int, toAltFt: Int, distanceNm: Double): Int {
        val altDiffFt = fromAltFt - toAltFt
        if (altDiffFt <= 0 || distanceNm <= 0) return 0

        val timeHours = distanceNm / 420.0 // assume average TAS ~ 420 knots
        val timeMinutes = timeHours * 60.0

        return (altDiffFt / timeMinutes).toInt().coerceIn(1000, 3000)
    }

    fun estimateTAS(gs: Int, trackDeg: Int, wather: WeatherData): Int {
        val windAngleRad = Math.toRadians((wather.windDirectionKts - trackDeg).toDouble())
        val windComponent = wather.windSpeedKts * Math.cos(windAngleRad)
        return (gs - windComponent).toInt()
    }

    fun tasToIAS(tas: Double, altitudeFt: Int): Double {
        // Approximate formula (valid up to around FL300)
        val ias = tas / Math.sqrt(1.0 + (altitudeFt / 145442.16))
        return ias
    }
}


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

    for (i in 1 until this.size) {
        val targetWaypoint = this[i]
        val bearing = currentPosition.bearingTo(targetWaypoint.position)

        val nextAltitudeConstraint = this.subList(i, this.size - 1).firstNotNullOfOrNull { starMap[it.id]?.starAltitudeConstraint?.exactFt }
        val targetAltitude = nextAltitudeConstraint ?: airfieldAltitude

        val totalDistance = currentPosition.distanceTo(targetWaypoint.position)
        val altitudeDelta = currentAltitude - targetAltitude

        var segmentStartPosition = currentPosition

        // Estimate descent distance required
        val requiredDescentDistance = if (altitudeDelta > 0) {
            aircraftPerformance.estimateDescentDistance(currentAltitude, targetAltitude)
        } else {
            0.0
        }

        // Only perform cruise if there is sufficient distance for it (before descent starts)
        if (totalDistance > requiredDescentDistance && altitudeDelta > 0) {
            // Cruise distance before descent starts
            val cruiseDistance = totalDistance - requiredDescentDistance

            // Cruise phase
            val weather = windData.interpolateWeatherAtAltitude(currentAltitude)
            val cruiseGs = aircraftPerformance.estimateGroundSpeed(currentAltitude, bearing, weather)
            val cruiseTimeMillis = (cruiseDistance / cruiseGs) * 3600 * 1000

            duration += cruiseTimeMillis.roundToLong().milliseconds

            // Move position to top-of-descent point
            segmentStartPosition = currentPosition.interpolatePositionAlongPath(targetWaypoint.position, cruiseDistance)

            println("Cruise: ${cruiseDistance.format(2)} NM at GS ${cruiseGs} kt = ${(cruiseTimeMillis / 1000).format(1)} s")
        }

        // Handle descent only after the cruise phase
        if (altitudeDelta > 0 && totalDistance > requiredDescentDistance) {
            // Descent phase
            val descentPath = aircraftPerformance.computeDescentPath(
                fromAltFt = currentAltitude,
                toAltFt = targetAltitude,
                from = segmentStartPosition,
                to = targetWaypoint.position
            )

            descentPath.forEach { step ->
                val stepWeather = windData.interpolateWeatherAtAltitude(step.altitudeFt)
                val stepGs = aircraftPerformance.estimateGroundSpeed(step.altitudeFt, bearing, stepWeather)
                val stepTimeMillis = (step.length / stepGs) * 3600 * 1000

                duration += stepTimeMillis.roundToLong().milliseconds
                currentAltitude = step.altitudeFt
                currentPosition = step.position

                println("Descent: ${step.length.format(2)} NM at GS ${stepGs} kt = ${(stepTimeMillis / 1000).format(1)} s, to: ${step.altitudeFt}")
            }
        }

        // Snap to waypoint at end of segment
        currentPosition = targetWaypoint.position
        println("Reached ${targetWaypoint.id}")
    }

    println("Estimated duration: ${duration.inWholeSeconds} seconds")
    return duration
}


/* *************** */

data class DescentStep(
    val position: LatLng,
    val altitudeFt: Int,
    val length: Float
)

fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

fun AircraftPerformance.computeDescentPath(
    fromAltFt: Int,
    toAltFt: Int,
    from: LatLng,
    to: LatLng
): List<DescentStep> {
    val descentPath = mutableListOf<DescentStep>()

    var currentAltitude = fromAltFt
    var currentPosition = from
    var remainingDistance = from.distanceTo(to)
    val track = from.bearingTo(to)

    while (currentAltitude > toAltFt && remainingDistance > 0) {
        val descentRateFpm = this.estimateDescentRate(currentAltitude)
        val verticalSpeed = descentRateFpm / 60.0 // ft/sec
        val groundspeedKts = this.estimateGroundSpeed(currentAltitude, track, null) // Approx for now
        val deltaTime = 10.0 // seconds

        val descentFt = verticalSpeed * deltaTime
        val distanceNm = (groundspeedKts.toFloat() * deltaTime) / 3600.0

        val nextAlt = max(currentAltitude - descentFt.roundToInt(), toAltFt)
        val nextPos = currentPosition.interpolatePositionAlongPath(to, distanceNm)

        descentPath.add(
            DescentStep(
                position = nextPos,
                altitudeFt = nextAlt,
                length = distanceNm.toFloat()
            )
        )

        currentAltitude = nextAlt
        currentPosition = nextPos
        remainingDistance -= distanceNm
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

fun AircraftPerformance.estimateDescentDistance(fromAltFt: Int, toAltFt: Int): Double {
    val altitudeDeltaFt = fromAltFt - toAltFt
    val descentDistanceNm = (altitudeDeltaFt / 1000.0) * 3.0  // 3 NM for every 1000 feet of descent

    return descentDistanceNm
}

data class AircraftPerformance(
    val idleDescentRateFpm: Int,
    val initialDescentSpeedIas: Int,
    val below10kSpeedIas: Int,
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

