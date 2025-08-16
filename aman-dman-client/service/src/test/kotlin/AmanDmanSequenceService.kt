import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.service.*
import no.vaccsca.amandman.service.AmanDmanSequenceService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AmanDmanSequenceService {

    @Test
    fun `Should insert arrival`() {
        val sequence = Sequence(emptyList())

        val mockArrival = AircraftSequenceCandidate(
            callsign = "TEST123",
            preferredTime = Clock.System.now() + 5.minutes,
            landingIas = 150,
            wakeCategory = 'M'
        )

        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(mockArrival))

        assert(updatedSequence.sequecencePlaces.find { it.item.id == "TEST123" } != null) { "Arrival should be inserted into sequence" }
    }


    @Test
    fun `Aircraft outside AAH should not be inserted`() {

        val sequence = Sequence(emptyList())

        val mockArrival = AircraftSequenceCandidate(
            callsign = "TEST123",
            preferredTime = Clock.System.now() + AmanDmanSequenceService.AAH_THRESHOLD + 5.minutes,
            landingIas = 150,
            wakeCategory = 'M'
        )

        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(mockArrival))

        assertEquals(updatedSequence.sequecencePlaces, emptyList())
    }

    @Test
    fun `Should not update sequence position when already sequenced`() {

        val now = Clock.System.now()

        val sequence = Sequence(
            sequecencePlaces = listOf(
                SequencePlace(
                    scheduledTime = now + 5.minutes,
                    item = makeSequenceCandidate(
                        callsign = "TEST123",
                        preferredTime = now + 5.minutes
                    )
                )
            )
        )

        val mockArrival = makeSequenceCandidate(
            callsign = "TEST123",
            preferredTime = now + 10.minutes,
        )

        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(mockArrival))

        assertEquals(updatedSequence.sequecencePlaces.size, 1)
        assertEquals(updatedSequence.sequecencePlaces.first().scheduledTime, now + 5.minutes)
    }

    @Test
    fun `When aircraft enters AAH, schedule it with sufficient spacing behind preceding`() {
        val leading = makeSequenceCandidate(
            callsign = "LEAD123",
            preferredTime = Clock.System.now() + 10.minutes,
            landingIas = 150,
            wakeCategory = 'M'
        )
        val following = makeSequenceCandidate(
            callsign = "FOLLOW123",
            preferredTime = Clock.System.now() + 10.minutes,
            landingIas = 150,
            wakeCategory = 'M'
        )

        val sequence = Sequence(
            sequecencePlaces = listOf(
                SequencePlace(
                    scheduledTime = leading.preferredTime,
                    item = leading
                )
            )
        )

        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(leading, following))

        assertEquals("FOLLOW123", updatedSequence.sequecencePlaces[1].item.id)
        assert(updatedSequence.sequecencePlaces[1].scheduledTime > leading.preferredTime)
    }

    @Test
    fun `When spacing is already sufficient, keep both preferred times`() {
        val leading = makeSequenceCandidate(
            callsign = "LEAD123",
            preferredTime = Clock.System.now() + 10.minutes,
            landingIas = 150,
            wakeCategory = 'M'
        )
        val following = makeSequenceCandidate(
            callsign = "FOLLOW123",
            preferredTime = Clock.System.now() + 20.minutes,
            landingIas = 150,
            wakeCategory = 'M'
        )

        val sequence = Sequence(emptyList())
        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(leading, following))

        assertEquals(2, updatedSequence.sequecencePlaces.size)
        assertEquals(leading.preferredTime, updatedSequence.sequecencePlaces[0].scheduledTime)
        assertEquals(following.preferredTime, updatedSequence.sequecencePlaces[1].scheduledTime)
    }

    @Test
    fun `Decide spacing based on wake category`() {
        // Arrange
        val now = Clock.System.now()
        val firstLanding = makeSequenceCandidate(
            callsign = "AIRCRAFT1",
            preferredTime = now + 10.minutes,
            wakeCategory = 'M'
        )
        val secondLanding = makeSequenceCandidate(
            callsign = "AIRCRAFT2",
            preferredTime = now + 10.minutes + 1.seconds,
            wakeCategory = 'H'
        )
        val thirdLanding = makeSequenceCandidate(
            callsign = "AIRCRAFT3",
            preferredTime = now + 10.minutes + 1.seconds,
            wakeCategory = 'M'
        )
        val sequence = Sequence(emptyList())

        // Act
        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(firstLanding, secondLanding, thirdLanding))

        // Assert
        assertEquals(3, updatedSequence.sequecencePlaces.size)
        assertEquals("AIRCRAFT1", updatedSequence.sequecencePlaces[0].item.id)
        assertEquals("AIRCRAFT2", updatedSequence.sequecencePlaces[1].item.id)
        assertEquals("AIRCRAFT3", updatedSequence.sequecencePlaces[2].item.id)

        val diffFirstAndSecond = updatedSequence.sequecencePlaces[1].scheduledTime - updatedSequence.sequecencePlaces[0].scheduledTime
        val diffSecondAndThird = updatedSequence.sequecencePlaces[2].scheduledTime - updatedSequence.sequecencePlaces[1].scheduledTime

        assert(diffFirstAndSecond < diffSecondAndThird) {
            "Expected more spacing between second and third aircraft due to wake category difference"
        }
    }

    @Test
    fun `Aircraft entering AAH at the same time should be spaced correctly`() {
        val now = Clock.System.now()
        val leading = makeSequenceCandidate(
            callsign = "LEAD123",
            preferredTime = now,
        )
        val following = makeSequenceCandidate(
            callsign = "FOLLOW123",
            preferredTime = now,
        )
        val sequence = Sequence(emptyList())

        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(leading, following))

        assertEquals(2, updatedSequence.sequecencePlaces.size)
        assertEquals("LEAD123", updatedSequence.sequecencePlaces[0].item.id)
        assertEquals("FOLLOW123", updatedSequence.sequecencePlaces[1].item.id)

        val timeDiff = updatedSequence.sequecencePlaces[1].scheduledTime - updatedSequence.sequecencePlaces[0].scheduledTime
        assert(timeDiff > 50.seconds)
    }

    @Test
    fun `When candidate spawns in AAH it can be placed between two sequenced arrivals`() {
        val now = Clock.System.now()
        val leading = makeSequenceCandidate(
            callsign = "LEAD123",
            preferredTime = now + 10.minutes,
        )
        val following = makeSequenceCandidate(
            callsign = "FOLLOW123",
            preferredTime = now + 20.minutes,
        )
        val candidate = makeSequenceCandidate(
            callsign = "CANDIDATE123",
            preferredTime = now + 15.minutes,
        )
        val sequence = Sequence(
            sequecencePlaces = listOf(
                SequencePlace(
                    scheduledTime = leading.preferredTime,
                    item = leading
                ),
                SequencePlace(
                    scheduledTime = following.preferredTime,
                    item = following
                )
            )
        )
        val updatedSequencePlaces = AmanDmanSequenceService.updateSequence(sequence, listOf(leading, following, candidate)).sequecencePlaces

        assertEquals(3, updatedSequencePlaces.size)
        assertEquals("LEAD123", updatedSequencePlaces[0].item.id)
        assertEquals("CANDIDATE123", updatedSequencePlaces[1].item.id)
        assertEquals("FOLLOW123", updatedSequencePlaces[2].item.id)
    }

    @Test
    fun `Should fail when inserting aircraft causes insufficient spacing with follower`() {
        val now = Clock.System.now()

        // Create a sequence with minimal but sufficient spacing
        val leading = makeSequenceCandidate(
            callsign = "LEAD123",
            preferredTime = now + 10.minutes,
            wakeCategory = 'M'  // Medium aircraft
        )
        val trailing = makeSequenceCandidate(
            callsign = "TRAIL123",
            preferredTime = now + 12.minutes,  // Only 2 minutes after leading (minimal spacing)
            wakeCategory = 'L'  // Light aircraft
        )

        val sequence = Sequence(
            sequecencePlaces = listOf(
                SequencePlace(scheduledTime = leading.preferredTime, item = leading),
                SequencePlace(scheduledTime = trailing.preferredTime, item = trailing)
            )
        )

        // Insert a Heavy aircraft that wants to go between them
        val newAircraft = makeSequenceCandidate(
            callsign = "NEW123",
            preferredTime = now + 11.minutes,  // Wants to go exactly between them
            wakeCategory = 'H'  // Heavy aircraft - requires MORE spacing behind it
        )

        val updatedSequence = AmanDmanSequenceService.updateSequence(sequence, listOf(newAircraft))

        val sortedPlaces = updatedSequence.sequecencePlaces.sortedBy { it.scheduledTime }

        // The critical check: Heavy -> Light requires 6nm spacing
        val heavyToLight = sortedPlaces.find { it.item.id == "NEW123" }!!
        val lightAircraft = sortedPlaces.find { it.item.id == "TRAIL123" }!!

        val actualSpacing = lightAircraft.scheduledTime - heavyToLight.scheduledTime
        val requiredSpacing = (6.0 / 150 * 3600).seconds  // Heavy->Light = 6nm at 150kt

        assert(actualSpacing >= requiredSpacing) {
            "Heavy->Light spacing violation: actual=${actualSpacing}, required=${requiredSpacing}"
        }
    }

    // Helper functions //////////////////////////////

    private fun makeSequenceCandidate(
        callsign: String,
        preferredTime: Instant,
        landingIas: Int = 150,
        wakeCategory: Char = 'M'
    ): AircraftSequenceCandidate {
        return AircraftSequenceCandidate(
            callsign = callsign,
            preferredTime = preferredTime,
            landingIas = landingIas,
            wakeCategory = wakeCategory
        )
    }


}