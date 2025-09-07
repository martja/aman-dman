package no.vaccsca.amandman.model

import no.vaccsca.amandman.model.data.repository.NavdataRepository
import no.vaccsca.amandman.model.data.dto.AmanDmanSettingsJson
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.model.data.repository.WeatherDataRepository
import no.vaccsca.amandman.model.domain.valueobjects.Star
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile

class AmanModel {

    val weatherDataRepository = WeatherDataRepository()
    val navdataRepository = NavdataRepository()

    fun getSettings(reload: Boolean = false): AmanDmanSettingsJson {
        return SettingsRepository.getSettings(reload)
    }

    fun getWeatherData(lat: Double, lng: Double): VerticalWeatherProfile? {
        return weatherDataRepository.getWindData(lat, lng)
    }

    fun getStars(): List<Star> {
        return navdataRepository.stars
    }
}