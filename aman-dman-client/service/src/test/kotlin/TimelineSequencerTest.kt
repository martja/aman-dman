import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.RunwayArrivalOccurrence
import kotlin.test.Test

class TimelineSequencerTest {
    val timelineSequencer = AmanDmanModel()

    @Test
    fun testSequenceArrivals() {

        val result = timelineSequencer.updateSequence(
            listOf(
                mockRunwayArrivalOccurrence("ABC001", Instant.parse("2023-10-01T10:00:00Z"), 10f),
                mockRunwayArrivalOccurrence("ABC002", Instant.parse("2023-10-01T10:05:00Z"), 20f),
                mockRunwayArrivalOccurrence("ABC003", Instant.parse("2023-10-01T10:02:00Z"), 30f),
                mockRunwayArrivalOccurrence("ABC004", Instant.parse("2023-10-01T10:03:00Z"), 40f),
            )
        )

        assert(result.size == 4) { "Expected 4 sequenced arrivals, got ${result.size}" }

    }

    fun mockRunwayArrivalOccurrence(
        mockCallsign: String,
        mockEstimatedTime: Instant,
        mockRemainingDistance: Float
    ) =
        mockk<RunwayArrivalOccurrence> {
            every { callsign } returns mockCallsign
            every { estimatedTime } returns mockEstimatedTime
            every { descentTrajectory } returns mockk {
                every { isEmpty() } returns false
                every { first().remainingDistance } returns mockRemainingDistance
            }
        }
}