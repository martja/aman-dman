import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.AmanDmanSequence
import kotlin.test.Test

class TimelineSequencerTest {
    val timelineSequence = AmanDmanSequence()

    @Test
    fun testSequenceArrivals() {

        val result = timelineSequence.updateSequence(
            listOf(
                mockRunwayArrivalEvent("ABC001", Instant.parse("2023-10-01T10:00:00Z"), 10f),
                mockRunwayArrivalEvent("ABC002", Instant.parse("2023-10-01T10:05:00Z"), 20f),
                mockRunwayArrivalEvent("ABC003", Instant.parse("2023-10-01T10:02:00Z"), 30f),
                mockRunwayArrivalEvent("ABC004", Instant.parse("2023-10-01T10:03:00Z"), 40f),
            )
        )

        assert(result.size == 4) { "Expected 4 sequenced arrivals, got ${result.size}" }

    }

    fun mockRunwayArrivalEvent(
        mockCallsign: String,
        mockEstimatedTime: Instant,
        mockRemainingDistance: Float
    ) =
        mockk<RunwayArrivalEvent> {
            every { callsign } returns mockCallsign
            every { estimatedTime } returns mockEstimatedTime
            every { descentTrajectory } returns mockk {
                every { isEmpty() } returns false
                every { first().remainingDistance } returns mockRemainingDistance
            }
        }
}