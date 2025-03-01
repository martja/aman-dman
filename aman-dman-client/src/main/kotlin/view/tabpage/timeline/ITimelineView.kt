package org.example.presentation.tabpage.timeline

import kotlinx.datetime.Instant
import java.awt.Rectangle

interface ITimelineView {
    fun calculateYPositionForInstant(instant: Instant): Int
    fun getRulerBounds(): Rectangle
}