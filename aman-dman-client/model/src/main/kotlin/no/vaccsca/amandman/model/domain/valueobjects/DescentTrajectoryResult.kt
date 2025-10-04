package no.vaccsca.amandman.model.domain.valueobjects

data class DescentTrajectoryResult(
    val trajectoryPoints: List<TrajectoryPoint>,
    val runway: RunwayInfo,
    val star: Star?
)
