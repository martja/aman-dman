package no.vaccsca.amandman.model.domain

import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.model.UserRole

data class TimelineGroup(
    val airportIcao: String,
    val name: String,
    val timelines: MutableList<TimelineConfig>,
    val userRole: UserRole
)