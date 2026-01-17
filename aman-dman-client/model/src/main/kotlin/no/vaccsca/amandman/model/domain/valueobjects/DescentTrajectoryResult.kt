package no.vaccsca.amandman.model.domain.valueobjects

data class DescentTrajectoryResult(
    val trajectoryPoints: List<TrajectoryPoint>,
    val runwayThreshold: RunwayThreshold,
    val star: Star?
)
