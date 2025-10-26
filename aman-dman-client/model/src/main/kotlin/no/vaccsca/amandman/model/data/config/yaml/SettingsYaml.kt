package no.vaccsca.amandman.model.data.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class AmanDmanSettingsYaml(
    val timelines: Map<String, List<TimelineYaml>>, // ICAO -> list of timelines
    val connectionConfig: ConnectionConfigYaml,
    val arrivalLabelLayouts: Map<String, List<LabelItemYaml>>,
    val departureLabelLayouts: Map<String, List<LabelItemYaml>>?
)

data class TimelineYaml(
    val timelineTitle: String,
    val left: SideYaml? = null,
    val right: SideYaml,
    val arrivalLabelLayoutId: String
)

data class SideYaml(
    val runways: List<String>,
)

data class ConnectionConfigYaml(
    val atcClient: AtcClientYaml,
    val api: ApiYaml
)

data class AtcClientYaml(
    val host: String,
    val port: Int
)

data class ApiYaml(
    val host: String
)

data class LabelItemYaml(
    val src: LabelItemSourceYaml,
    val w: Int,
    val align: LabelItemAlignmentYaml? = null,
    val def: String? = null,
    val maxLen: Int? = null,
)

enum class LabelItemAlignmentYaml(@JsonValue val value: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): LabelItemAlignmentYaml? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

enum class LabelItemSourceYaml(@JsonValue val value: String) {
    CALL_SIGN("callSign"),
    ASSIGNED_RUNWAY("assignedRunway"),
    ASSIGNED_STAR("assignedStar"),
    AIRCRAFT_TYPE("aircraftType"),
    WAKE_CATEGORY("wakeCategory"),
    TIME_BEHIND_PRECEDING("timeBehindPreceding"),
    TIME_BEHIND_PRECEDING_ROUNDED("minutesBehindPrecedingRounded"),
    REMAINING_DISTANCE("remainingDistance"),
    DISTANCE_BEHIND_PRECEDING("distanceBehindPreceding"),
    DIRECT_ROUTING("directRouting"),
    SCRATCH_PAD("scratchPad"),
    ESTIMATED_LANDING_TIME("estimatedLandingTime"),
    GROUND_SPEED("groundSpeed"),
    GROUND_SPEED_10("groundSpeed10"),
    ALTITUDE("altitude"),
    TTL_TTG("timeToLoseOrGain");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): LabelItemSourceYaml? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}