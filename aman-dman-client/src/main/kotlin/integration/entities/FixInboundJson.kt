package org.example.integration.entities

data class DescentProfileJson(
    val minAltitude: Int,
    val maxAltitude: Int,
    val averageHeading: Int,
    val secDuration: Int,
    val distance: Float
)

data class FixInboundJson(
    val callsign: String,
    val icaoType: String,
    val wtc: Char,
    val runway: String,
    val star: String,
    val eta: Long,
    val finalFixEta: Long,
    val remainingDist: Float,
    val finalFix: String,
    val viaFix: String,
    val flightLevel: Int,
    val pressureAltitude: Int,
    val groundSpeed: Int,
    val secondsBehindPreceeding: Int,
    val isAboveTransAlt: Boolean,
    val trackedByMe: Boolean,
    val direct: String,
    val scratchPad: String,
    val descentProfile: List<DescentProfileJson>
)