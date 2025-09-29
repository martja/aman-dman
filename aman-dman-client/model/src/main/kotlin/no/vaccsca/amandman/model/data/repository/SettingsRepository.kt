package no.vaccsca.amandman.model.data.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.vaccsca.amandman.model.data.dto.AirportDataJson
import no.vaccsca.amandman.model.data.dto.AmanDmanSettingsJson
import java.io.File

object SettingsRepository {

    private var settingsJson: AmanDmanSettingsJson? = null
    private var airportDataJson: AirportDataJson? = null

    // Path to the settings file on classpath
    private const val SETTINGS_FILE_PATH = "config/settings.json"
    private const val AIRPORTS_FILE_PATH = "config/airports.json"

    init {
        loadAll()
    }

    fun loadAll() {
        loadSettings()
        loadAirportData()
    }

    fun getSettings(reload: Boolean = false): AmanDmanSettingsJson {
        if (settingsJson == null || reload) {
            loadSettings()
        }
        return settingsJson!!
    }

    fun getAirportData(reload: Boolean = false): AirportDataJson {
        if (airportDataJson == null || reload) {
            loadAirportData()
        }
        return airportDataJson!!
    }

    fun updateSettings(newSettings: AmanDmanSettingsJson) {
        settingsJson = newSettings
        saveSettings()
    }

    // Load settings from JSON file
    private fun loadSettings() {
        val localFile = File(SETTINGS_FILE_PATH)
        val inputStream = localFile.inputStream()

        inputStream.use {
            settingsJson = jacksonObjectMapper().readValue(it, AmanDmanSettingsJson::class.java)
        }
    }

    private fun loadAirportData() {
        val localFile = File(AIRPORTS_FILE_PATH)
        val inputStream = localFile.inputStream()

        inputStream.use {
            airportDataJson = jacksonObjectMapper().readValue(it, AirportDataJson::class.java)
        }
    }

    private fun saveSettings() {
        val jsonFile = File(SETTINGS_FILE_PATH)
        jsonFile.writeText(jacksonObjectMapper().writeValueAsString(settingsJson))
    }
}