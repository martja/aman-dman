package no.vaccsca.amandman.model.data.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.CdmData
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.ZonedDateTime


class CdmClient {
    // https://cdm-server-production.up.railway.app/ifps/depAirport?airport=ENGM

    private val httpClient = OkHttpClient()

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class CdmJson(
        val callsign: String,
        val cdmData: CdmDataJson,
        val timeStamp: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class CdmDataJson(
        val tobt: String?,
        val tsat: String?,
        val ttot: String?,
        val ctot: String?,
    )

    fun fetchCdmDepartures(airportIcao: String): List<CdmData>? {
        val request = Request.Builder()
            .url("https://cdm-server-production.up.railway.app/ifps/depAirport?airport=$airportIcao")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()

            // Parse response body
            val body = response.body.string()
            val objectMapper = ObjectMapper().registerKotlinModule()

            return try {
                val reader = objectMapper.readerFor(CdmJson::class.java)
                reader.readValues<CdmJson>(body).readAll().toList().map {
                    CdmData(
                        callsign = it.callsign,
                        ttot = it.cdmData.ttot?.let { parseHhMmSsTimestamp(it) },
                        ctot = it.cdmData.ctot?.let { parseHhMmSsTimestamp(it) },
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Parses a timestamp in "HHMMSS" format to an Instant (UTC)
     */
    private fun parseHhMmSsTimestamp(value: String): Instant? {
        return try {
            val hours = value.substring(0, 2).toInt()
            val minutes = value.substring(2, 4).toInt()
            val seconds = value.substring(4, 6).toInt()

            val now = ZonedDateTime.now()
            var dateTime = now.withHour(hours).withMinute(minutes).withSecond(seconds).withNano(0)

            // If the time was already passed an hour ago, assume it's for the next day
            if (dateTime.isBefore(now.minusHours(1))) {
                dateTime = dateTime.plusDays(1)
            }
            Instant.parse(dateTime.toInstant().toString())
        } catch (e: Exception) {
            null
        }
    }
}