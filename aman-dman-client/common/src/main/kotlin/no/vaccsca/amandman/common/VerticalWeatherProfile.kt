package no.vaccsca.amandman.common

import kotlinx.datetime.Instant

data class VerticalWeatherProfile(
    val time: Instant,
    val position: LatLng,
    val weatherLayers: MutableList<WeatherLayer>
)