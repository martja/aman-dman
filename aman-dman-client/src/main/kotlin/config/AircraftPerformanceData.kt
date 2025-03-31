package org.example.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.model.entities.performance.AircraftPerformance
import java.io.File
import java.io.FileNotFoundException

object AircraftPerformanceData {

    private val all: List<AircraftPerformance>

    // Path to the settings file on classpath
    private const val SETTINGS_FILE_PATH = "aircraft_performance.json"

    init {
        val jsonFile = this::class.java.classLoader.getResource(SETTINGS_FILE_PATH)?.file.let { File(it) }
        if (!jsonFile.exists()) {
            throw FileNotFoundException("Settings file not found: $SETTINGS_FILE_PATH")
        }
        all = jacksonObjectMapper().readValue( jsonFile.readText())
    }

    fun get(icao: String): AircraftPerformance {
        return all.find { it.ICAO == icao } ?: throw IllegalArgumentException("No aircraft performance data for ICAO $icao")
    }

}