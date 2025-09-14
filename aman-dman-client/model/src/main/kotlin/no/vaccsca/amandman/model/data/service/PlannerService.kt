package no.vaccsca.amandman.model.data.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint

abstract class PlannerService(
    val airportIcao: String
) {
    abstract fun planArrivals()
    abstract fun setMinimumSpacing(minimumSpacingDistanceNm: Double): Result<Unit>
    abstract fun refreshWeatherData(): Result<Unit>
    abstract fun suggestScheduledTime(callsign: String, scheduledTime: Instant): Result<Unit>
    abstract fun reSchedule(callSign: String? = null): Result<Unit>
    abstract fun isTimeSlotAvailable(callsign: String, scheduledTime: Instant): Result<Boolean>
    abstract fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?>
    abstract fun stop()
}