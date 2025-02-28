package model.entities.json

import org.example.integration.entities.TimelineAircraftJson

data class DataPackageJson(
    val arrivals: List<TimelineAircraftJson>
)
