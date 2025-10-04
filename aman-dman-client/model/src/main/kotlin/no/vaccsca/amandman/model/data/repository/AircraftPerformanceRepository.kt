package no.vaccsca.amandman.model.data.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.vaccsca.amandman.model.domain.valueobjects.AircraftPerformance
import java.io.File
import java.io.FileNotFoundException

object AircraftPerformanceData {

    // Path to the external config file
    private const val CONFIG_FILE_PATH = "config/aircraft_performance.json"

    private val all by lazy {
        loadSettingsFromFile(CONFIG_FILE_PATH)
    }

    fun get(icao: String): AircraftPerformance {
        return all.find { it.ICAO == icao }
            ?: throw IllegalArgumentException("No aircraft performance data for ICAO $icao")
    }

    fun loadSettingsFromFile(filePath: String): List<AircraftPerformance> {
        val jsonFile = File(filePath)
        if (!jsonFile.exists()) {
            throw FileNotFoundException("Settings file not found at: $filePath")
        }
        return jacksonObjectMapper().readValue(jsonFile.readText())
    }
}