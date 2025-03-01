package org.example.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException

object SettingsManager {

    private var settingsJson: AmanDmanSettingsJson? = null

    // Path to the settings file on classpath
    private const val SETTINGS_FILE_PATH = "settings.json"

    init {
        loadSettings()
    }

    fun getSettings(): AmanDmanSettingsJson {
        return settingsJson!!
    }

    fun updateSettings(newSettings: AmanDmanSettingsJson) {
        settingsJson = newSettings
        saveSettings()
    }

    // Load settings from JSON file
    private fun loadSettings() {
        val jsonFile = this::class.java.classLoader.getResource(SETTINGS_FILE_PATH)?.file.let { File(it) }
        if (!jsonFile.exists()) {
            throw FileNotFoundException("Settings file not found: $SETTINGS_FILE_PATH")
        }
        settingsJson = jacksonObjectMapper().readValue( jsonFile.readText())
    }

    private fun saveSettings() {
        val jsonFileName = "settings.json"
        val jsonFile = File(jsonFileName)
        jsonFile.writeText(jacksonObjectMapper().writeValueAsString(settingsJson))
    }
}