package entity

import kotlinx.datetime.Instant

data class TimeRange(
    val start: Instant,
    val end: Instant
)