package controller

import org.example.controller.calculateWindTimeAdjustmentInSegment
import org.example.model.DescentProfileSegment
import org.example.model.entities.weather.Wind
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimelineControllerTest {

    @Test
    fun test_calculateSectorDelay() {
        val sector = DescentProfileSegment(15000, 20000, 180, 10.minutes, 30.0f)

        val adjustmentInHeadwind = calculateWindTimeAdjustmentInSegment(sector, Wind(180, 60))

        assertTrue(adjustmentInHeadwind > 0.seconds) // Expect delay

        val adjustmentInTailwind = calculateWindTimeAdjustmentInSegment(sector, Wind(0, 60))

        assertTrue(adjustmentInTailwind < 0.seconds) // Expect acceleration

        val adjustmentInRightCrosswind = calculateWindTimeAdjustmentInSegment(sector, Wind(270, 60))

        assertEquals(0.seconds, adjustmentInRightCrosswind) // Expect no change

        val adjustmentInLeftCrosswind = calculateWindTimeAdjustmentInSegment(sector, Wind(90, 60))

        assertEquals(0.seconds, adjustmentInLeftCrosswind) // Expect no change
    }
}