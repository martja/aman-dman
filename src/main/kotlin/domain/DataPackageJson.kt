package domain

data class DataPackageJson(
    val arrivals: List<AircraftInboundJson>
)

data class AircraftInboundJson(
    val callsign: String,
    val icaoType: String,
    val wtc: Char,
    val runway: String,
    val star: String,
    val eta: Long,
    val remainingDist: Float,
    val finalFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val secondsBehindPreceeding: Int,
    val isAboveTransAlt: Boolean,
    val trackedByMe: Boolean,
    val direct: String,
    val scratchPad: String,
)