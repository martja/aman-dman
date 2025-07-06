package config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.vaccsca.amandman.common.AircraftPerformance
import java.io.File
import java.io.FileNotFoundException

object AircraftPerformanceData {

    private val all: List<AircraftPerformance>

    // Path to the external config file
    private const val CONFIG_FILE_PATH = "config/aircraft_performance.json"

    init {
        val jsonFile = File(CONFIG_FILE_PATH)
        if (!jsonFile.exists()) {
            throw FileNotFoundException("Settings file not found at: $CONFIG_FILE_PATH")
        }
        all = jacksonObjectMapper().readValue(jsonFile.readText())
    }

    fun get(icao: String): AircraftPerformance {
        return all.find { it.ICAO == icao }
            ?: throw IllegalArgumentException("No aircraft performance data for ICAO $icao")
    }
}