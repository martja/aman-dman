package org.example.integration

import kotlinx.datetime.*
import org.example.format
import org.example.model.entities.VerticalWindProfile
import org.example.model.entities.WindInformation
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class WindProfileGridPoint(
    val latitude: Double,
    val longitude: Double,
    val windProfile: VerticalWindProfile
)

data class BoundingBox(
    val topLat: Double,
    val bottomLat: Double,
    val leftLon: Double,
    val rightLon: Double
)

class WindApi {

    fun getVerticalProfileAtPoint(latitude: Double, longitude: Double): VerticalWindProfile? {
        val gridPoints = getVerticalProfileGrid(BoundingBox(
            topLat = latitude + 0.5,
            bottomLat = latitude - 0.5,
            leftLon = longitude - 0.5,
            rightLon = longitude + 0.5
        ))

        val closestPoint =
            gridPoints.minByOrNull {
                abs(it.latitude - latitude) + abs(it.longitude - longitude)
            }

        return closestPoint?.windProfile
    }


    private fun getVerticalProfileGrid(bbox: BoundingBox): MutableList<WindProfileGridPoint> {
        val (publishTime, grib) = fetchMostRecentForecast(bbox)!!

        val latitudes = grib.findVariable("lat")!!
        val longitudes = grib.findVariable("lon")!!
        val reftimes = grib.findVariable("time")!!
        val isobarics = grib.findVariable("isobaric")!!
        val temperatures = grib.findVariable("Temperature_isobaric")!!
        val uWindComponents = grib.findVariable("u-component_of_wind_isobaric")!!
        val vWindComponents = grib.findVariable("v-component_of_wind_isobaric")!!

        // Read all data arrays once
        val latData = latitudes.read()
        val lonData = longitudes.read()
        val timeData = reftimes.read()
        val isobaricData = isobarics.read()
        val tempData = temperatures.read()
        val uWindData = uWindComponents.read()
        val vWindData = vWindComponents.read()

        val gridPoints = mutableListOf<WindProfileGridPoint>()

        val i = 0 // Only one time dimension

        for (a in 0 until isobaricData.shape[0]) {
            for (j in 0 until latData.shape[0]) {
                for (k in 0 until lonData.shape[0]) {

                    val isobaric = isobaricData.getDouble(a)
                    val gridLat = latData.getDouble(j)
                    val gridLon = lonData.getDouble(k)

                    // Wind dimension order: [time][isobaric][lat][lon]
                    val uWind = uWindData.getFloat(uWindData.index.set(i, a, j, k))
                    val vWind = vWindData.getFloat(vWindData.index.set(i, a, j, k))

                    val windDirection = Math.toDegrees(atan2(uWind, vWind).toDouble()).roundToInt()
                    val windSpeedKnots = (sqrt(uWind * uWind + vWind * vWind) * 1.94384).roundToInt()

                    // Temperature dimension order: [time][isobaric][lat][lon]
                    val temp = (tempData.getDouble(tempData.index.set(i, a, j, k)) - 272.15).roundToInt()

                    val flightLevel = (pressureToAltitudeInFeet(isobaric) / 100).roundToInt()

                    val forecastTime = publishTime.plus(timeData.getLong(i).hours)

                    val currentGridPoint = gridPoints.find { it.latitude == gridLat && it.longitude == gridLon }?.windProfile
                        ?: VerticalWindProfile(forecastTime, gridLat, gridLon, mutableListOf())

                    currentGridPoint.windInformation.add(WindInformation(flightLevel, windDirection, windSpeedKnots, temp))

                    if (gridPoints.none { it.latitude == gridLat && it.longitude == gridLon }) {
                        gridPoints.add(WindProfileGridPoint(gridLat, gridLon, currentGridPoint))
                    }
                }
            }
        }

        return gridPoints
    }

    private fun fetchMostRecentForecast(bbox: BoundingBox): Pair<Instant, NetcdfFile>? {
        val timeNow = Clock.System.now()
        var closestPublishTime = timeNow.minus((timeNow.epochSeconds % (6 * 60 * 60)).seconds)

        // Go backwards in time until we find a forecast
        for (i in 0 until 5) {
            try {
                val hoursSincePublishTime = (timeNow.epochSeconds - closestPublishTime.epochSeconds).toInt() / 3600
                val hoursSincePublishTime3hour = (hoursSincePublishTime / 3) * 3
                val gribFile = fetchGribFile(bbox = bbox, forecastTime = closestPublishTime, hourOffset = hoursSincePublishTime3hour)
                return Pair(closestPublishTime, gribFile)
            } catch (e: FileNotFoundException) {
                println("No forecast found for $closestPublishTime")
                closestPublishTime = closestPublishTime.minus(6.hours)
            }
        }
        return null
    }

    private fun fetchGribFile(bbox: BoundingBox, forecastTime: Instant, hourOffset: Int): NetcdfFile {
        val formattedForecastDate = forecastTime.format("yyyyMMdd")
        val formattedForecastHour = forecastTime.format("HH")
        val hoursOffsetFormatted = hourOffset.toString().padStart(3, '0')

        val fileUrl =
            "https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p50.pl" +
                    "?dir=%2Fgfs.${formattedForecastDate}%2F${formattedForecastHour}%2Fatmos" +
                    "&file=gfs.t${formattedForecastHour}z.pgrb2full.0p50.f$hoursOffsetFormatted" +
                    "&subregion=" +
                    "&toplat=${bbox.topLat}" +
                    "&leftlon=${bbox.leftLon}" +
                    "&rightlon=${bbox.rightLon}" +
                    "&bottomlat=${bbox.bottomLat}" +
                    "&lev_1000_mb" +
                    "&lev_850_mb" +
                    "&lev_700_mb" +
                    "&lev_650_mb" +
                    "&lev_600_mb" +
                    "&lev_550_mb" +
                    "&lev_500_mb" +
                    "&lev_450_mb" +
                    "&lev_400_mb" +
                    "&lev_375_mb" +
                    "&lev_350_mb" +
                    "&lev_325_mb" +
                    "&lev_300_mb" +
                    "&lev_275_mb" +
                    "&lev_250_mb" +
                    "&lev_225_mb" +
                    "&lev_200_mb" +
                    "&lev_175_mb" +
                    "&var_TMP=on" +
                    "&var_UGRD=on" +
                    "&var_VGRD=on"

        val tempFile = Files.createTempFile("wind_data", ".grib2").toFile()
        tempFile.deleteOnExit()

        URI(fileUrl).toURL().openStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return NetcdfFiles.open(tempFile.path) // now it's a real file
    }
}

fun pressureToAltitudeInFeet(pascal: Double): Double {
    return 145366.45 * (1 - Math.pow(pascal / 101325.0, 0.190284))
}