package model.entities.json

import org.example.integration.entities.FixInboundJson

data class DataPackageJson(
    val arrivals: List<FixInboundJson>
)
