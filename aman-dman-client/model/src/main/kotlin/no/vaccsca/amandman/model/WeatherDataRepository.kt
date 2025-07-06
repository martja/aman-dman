package no.vaccsca.amandman.model

import no.vaccsca.amandman.common.VerticalWeatherProfile
import no.vaccsca.amandman.integration.weather.WindApi

class WeatherDataRepository(
    private val windApi: WindApi = WindApi()
) {
    fun getWindData(lat: Double, lng: Double): VerticalWeatherProfile? {
        return windApi.getVerticalProfileAtPoint(lat, lng)
    }
}