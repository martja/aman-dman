package no.vaccsca.amandman.model.data.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint

interface PlannerService {
    fun planArrivalsFor(airportIcao: String)
    fun setMinimumSpacing(minimumSpacingDistanceNm: Double)
    fun refreshWeatherData(airportIcao: String, lat: Double, lon: Double)
    fun suggestScheduledTime(sequenceId: String, callsign: String, scheduledTime: Instant)
    fun reSchedule(sequenceId: String, callSign: String? = null)
    fun isTimeSlotAvailable(sequenceId: String, callsign: String, scheduledTime: Instant): Boolean
    fun getDescentProfileForCallsign(callsign: String): List<TrajectoryPoint>?
}