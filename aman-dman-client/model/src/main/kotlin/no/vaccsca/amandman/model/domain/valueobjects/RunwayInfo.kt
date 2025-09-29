package no.vaccsca.amandman.model.domain.valueobjects

data class RunwayInfo(
    val id: String,
    val latLng: LatLng,
    val elevation: Float,
    val trueHeading: Float
)