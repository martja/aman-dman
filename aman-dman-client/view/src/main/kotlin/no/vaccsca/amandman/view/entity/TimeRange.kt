package no.vaccsca.amandman.view.entity

import kotlinx.datetime.Instant

data class TimeRange(
    val start: Instant,
    val end: Instant
)