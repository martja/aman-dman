package org.example.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException

object SettingsManager {

    private var settingsJson: AmanDmanSettingsJson? = null

    // Path to the settings file on classpath
    private const val SETTINGS_FILE_PATH = "config/settings.json"

    init {
        loadSettings()
    }

    fun getSettings(reload: Boolean): AmanDmanSettingsJson {
        if (settingsJson == null || reload) {
            loadSettings()
        }
        return settingsJson!!
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

    private fun saveSettings() {
        val jsonFile = File(SETTINGS_FILE_PATH)
        jsonFile.writeText(jacksonObjectMapper().writeValueAsString(settingsJson))
    }
}