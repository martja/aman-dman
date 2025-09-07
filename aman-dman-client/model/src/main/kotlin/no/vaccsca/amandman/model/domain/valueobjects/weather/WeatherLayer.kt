package no.vaccsca.amandman.model.domain.valueobjects.weather

data class WeatherLayer(
    val flightLevelFt: Int,
    val temperatureC: Int,
    val windVector: WindVector
)