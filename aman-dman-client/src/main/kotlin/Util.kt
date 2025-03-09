package org.example

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime

@OptIn(FormatStringsInDatetimeFormats::class)
fun Instant.format(pattern: String): String {
    val dateTimeFormat = LocalDateTime.Format { byUnicodePattern(pattern) }
    return dateTimeFormat.format(this.toLocalDateTime(TimeZone.UTC))
}