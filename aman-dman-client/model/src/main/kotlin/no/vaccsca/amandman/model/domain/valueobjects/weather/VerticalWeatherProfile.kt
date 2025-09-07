package no.vaccsca.amandman.model.domain.valueobjects.weather

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.LatLng

data class VerticalWeatherProfile(
    val time: Instant,
    val position: LatLng,
    val weatherLayers: MutableList<WeatherLayer>
)