package controller

import org.example.controller.calculateWindTimeAdjustmentInSegment
import org.example.model.DescentProfileSegment
import org.example.model.entities.WindData
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimelineControllerTest {

    @Test
    fun test_calculateSectorDelay() {
        val sector = DescentProfileSegment(15000, 20000, 180, 10.minutes, 30.0f)

        val adjustmentInHeadwind = calculateWindTimeAdjustmentInSegment(sector, WindData(180, 60))

        assertTrue(adjustmentInHeadwind > 0.seconds) // Expect delay

        val adjustmentInTailwind = calculateWindTimeAdjustmentInSegment(sector, WindData(0, 60))

        assertTrue(adjustmentInTailwind < 0.seconds) // Expect acceleration

        val adjustmentInRightCrosswind = calculateWindTimeAdjustmentInSegment(sector, WindData(270, 60))

        assertEquals(0.seconds, adjustmentInRightCrosswind) // Expect no change

        val adjustmentInLeftCrosswind = calculateWindTimeAdjustmentInSegment(sector, WindData(90, 60))

        assertEquals(0.seconds, adjustmentInLeftCrosswind) // Expect no change
    }
}