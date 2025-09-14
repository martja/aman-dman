package no.vaccsca.amandman.model.data.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint

interface PlannerService {
    fun planArrivalsFor(airportIcao: String)
    fun setMinimumSpacing(airportIcao: String, minimumSpacingDistanceNm: Double): Result<Unit>
    fun refreshWeatherData(airportIcao: String, lat: Double, lon: Double): Result<Unit>
    fun suggestScheduledTime(sequenceId: String, callsign: String, scheduledTime: Instant): Result<Unit>
    fun reSchedule(sequenceId: String, callSign: String? = null): Result<Unit>
    fun isTimeSlotAvailable(sequenceId: String, callsign: String, scheduledTime: Instant): Result<Boolean>
    fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?>
}