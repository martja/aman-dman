package no.vaccsca.amandman.model.data.repository

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.vaccsca.amandman.model.data.dto.AirportDataJson
import no.vaccsca.amandman.model.data.dto.AmanDmanSettingsJson
import java.io.File

object SettingsRepository {

    private var settingsYaml: AmanDmanSettingsJson? = null
    private var airportDataYaml: AirportDataJson? = null

    // Paths to the YAML config files
    private const val SETTINGS_FILE_PATH = "config/settings.yaml"
    private const val AIRPORTS_FILE_PATH = "config/airports.yaml"

    private val yamlMapper = YAMLMapper().apply { registerKotlinModule() }

    init {
        loadAll()
    }

    fun loadAll() {
        loadSettings()
        loadAirportData()
    }

    fun getSettings(reload: Boolean = false): AmanDmanSettingsJson {
        if (settingsYaml == null || reload) {
            loadSettings()
        }
        return settingsYaml!!
    }

    fun getAirportData(reload: Boolean = false): AirportDataJson {
        if (airportDataYaml == null || reload) {
            loadAirportData()
        }
        return airportDataYaml!!
    }

    fun updateSettings(newSettings: AmanDmanSettingsJson) {
        settingsYaml = newSettings
        saveSettings()
    }

    /** Load settings from YAML file */
    private fun loadSettings() {
        val localFile = File(SETTINGS_FILE_PATH)
        if (!localFile.exists()) {
            throw IllegalStateException("Settings YAML file not found: $SETTINGS_FILE_PATH")
        }

        localFile.inputStream().use {
            settingsYaml = yamlMapper.readValue(it, AmanDmanSettingsJson::class.java)
        }
    }

    /** Load airport data from YAML file */
    private fun loadAirportData() {
        val localFile = File(AIRPORTS_FILE_PATH)
        if (!localFile.exists()) {
            throw IllegalStateException("Airport YAML file not found: $AIRPORTS_FILE_PATH")
        }

        localFile.inputStream().use {
            airportDataYaml = yamlMapper.readValue(it, AirportDataJson::class.java)
        }
    }

    /** Save settings back to YAML */
    private fun saveSettings() {
        val yamlFile = File(SETTINGS_FILE_PATH)
        yamlFile.writeText(yamlMapper.writeValueAsString(settingsYaml))
    }
}
