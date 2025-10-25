package no.vaccsca.amandman.model.domain.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.data.integration.SharedState
import no.vaccsca.amandman.model.domain.exception.UnsupportedInSlaveModeException
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import java.util.Timer
import java.util.TimerTask

class PlannerServiceSlave(
    airportIcao: String,
    private val sharedState: SharedState,
    private val dataUpdateListener: DataUpdateListener
) : PlannerService(airportIcao) {
    val timer = Timer()
    val arrivalAirportsToFetch = mutableSetOf<String>()

    override fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                fetchAll()
            }
        }, 0, 1000)
    }

    override fun getAvailableRunways(): Result<List<String>> =
        runCatching {
            throw UnsupportedInSlaveModeException("Descent profile cannot be viewed in slave mode")
        }

    private fun fetchAll() {
        for (airport in arrivalAirportsToFetch) {
            fetchAmanData(airport)
        }
    }

    private fun fetchAmanData(airportIcao: String) {
        try {
            val data = sharedState.getTimelineEvents(airportIcao)
            dataUpdateListener.onLiveData(airportIcao, data)
        } catch (e: Exception) {
            println("Failed to fetch timeline events for $airportIcao: ${e.message}")
        }

        try {
            val runwayStatuses = sharedState.getRunwayStatuses(airportIcao)
            dataUpdateListener.onRunwayModesUpdated(airportIcao, runwayStatuses)
        } catch (e: Exception) {
            println("Failed to fetch runway statuses for $airportIcao: ${e.message}")
        }

        try {
            val weatherData = sharedState.getWeatherData(airportIcao)
            dataUpdateListener.onWeatherDataUpdated(airportIcao, weatherData)
        } catch (e: Exception) {
            println("Failed to fetch weather data for $airportIcao: ${e.message}")
        }

        try {
            val minimumSpacingNm = sharedState.getMinimumSpacing(airportIcao)
            dataUpdateListener.onMinimumSpacingUpdated(airportIcao, minimumSpacingNm)
        } catch (e: Exception) {
            println("Failed to fetch minimum spacing data for $airportIcao: ${e.message}")
        }
    }

    override fun planArrivals() {
        if (!arrivalAirportsToFetch.contains(airportIcao)) {
            arrivalAirportsToFetch.add(airportIcao)
        }
        fetchAll()
    }

    override fun setMinimumSpacing(minimumSpacingDistanceNm: Double): Result<Unit> =
        runCatching {
            throw UnsupportedInSlaveModeException("Minimum spacing cannot be changed in slave mode")
        }

    override fun refreshWeatherData(): Result<Unit> =
        runCatching {
             throw UnsupportedInSlaveModeException("Weather data update cannot be triggered in slave mode")
        }

    override fun suggestScheduledTime(
        timelineEvent: TimelineEvent, scheduledTime: Instant, newRunway: String?
    ): Result<Unit> =
        runCatching {
            throw UnsupportedInSlaveModeException("Sequence cannot be changed in slave mode")
        }

    override fun reSchedule(callSign: String?): Result<Unit> =
        runCatching {
            throw UnsupportedInSlaveModeException("Sequence cannot be changed in slave mode")
        }

    override fun isTimeSlotAvailable(
        timelineEvent: TimelineEvent,
        scheduledTime: Instant
    ): Result<Boolean> =
        runCatching {
            throw UnsupportedInSlaveModeException("Sequence cannot be changed in slave mode")
        }

    override fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?> =
        runCatching {
            throw UnsupportedInSlaveModeException("Descent profile cannot be viewed in slave mode")
        }

    override fun stop() {
        timer.cancel()
    }
}