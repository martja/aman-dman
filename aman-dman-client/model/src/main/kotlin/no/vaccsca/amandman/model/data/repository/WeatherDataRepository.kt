package no.vaccsca.amandman.model.data.repository

import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile


class WeatherDataRepository(
    private val windApi: WindApi = WindApi()
) {
    fun getWindData(lat: Double, lng: Double): VerticalWeatherProfile? {
        return windApi.getVerticalProfileAtPoint(lat, lng)
    }
}