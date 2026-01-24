package no.vaccsca.amandman.model.domain.valueobjects.sequence

data class Sequence(
    /**
     * A map that tracks whether an aircraft is currently within the Sequencing Horizon.
     * Once an aircraft is in the sequencing horizon, it will be sequenced automatically and given a final scheduled time.
     */
    //val sequencingHorizon: Map<String, Boolean>,
    /**
     * A map that holds the current sequence of aircraft, where the key is the callsign
     * and the value is the scheduled time for that aircraft.
     */
    val sequecencePlaces: List<SequencePlace>
)