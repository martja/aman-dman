package no.vaccsca.amandman.model.domain.exception

open class DescentTrajectoryException(open val msg: String) : Exception(msg)

class ReachedEndOfRouteException(override val msg: String) : DescentTrajectoryException(msg)

class NoAssignedRunwayException(override val msg: String) : DescentTrajectoryException(msg)

class UnknownAircraftTypeException(override val msg: String) : DescentTrajectoryException(msg)

class EmptyTrajectoryException(override val msg: String) : DescentTrajectoryException(msg)