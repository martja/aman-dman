package org.example.presentation.tabpage.timeline

import java.awt.Rectangle
import java.time.Instant

interface ITimelineView {
    fun calculateYPositionForInstant(instant: Instant): Int
    fun getRulerBounds(): Rectangle
}