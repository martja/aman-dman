package no.vaccsca.amandman.model.data.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.data.service.integration.SharedStateHttpClient
import no.vaccsca.amandman.model.domain.exception.UnsupportedInSlaveModeException
import no.vaccsca.amandman.model.domain.service.DataUpdateListener
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import java.util.*

class PlannerServiceSlave(
    private val sharedStateHttpClient: SharedStateHttpClient,
    private val dataUpdateListener: DataUpdateListener
) : PlannerService {
    val timer = Timer()
    val arrivalAirportsToFetch = mutableSetOf<String>()

    init {
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                fetchAll()
            }
        }, 0, 1000)
    }

    private fun fetchAll() {
        for (airport in arrivalAirportsToFetch) {
            fetchAmanData(airport)
        }
    }

    private fun fetchAmanData(airportIcao: String) {
        val data = sharedStateHttpClient.getTimelineEvents(airportIcao)
        dataUpdateListener.onLiveData(airportIcao, data)

        val runwayStatuses = sharedStateHttpClient.getRunwayStatuses(airportIcao)
        dataUpdateListener.onRunwayModesUpdated(airportIcao, runwayStatuses)

        val weatherData = sharedStateHttpClient.getWeatherData(airportIcao)
        dataUpdateListener.onWeatherDataUpdated(airportIcao, weatherData)

        val minimumSpacingNm = sharedStateHttpClient.getMinimumSpacing(airportIcao)
        dataUpdateListener.onMinimumSpacingUpdated(airportIcao, minimumSpacingNm)
    }

    override fun planArrivalsFor(airportIcao: String) {
        if (!arrivalAirportsToFetch.contains(airportIcao)) {
            arrivalAirportsToFetch.add(airportIcao)
        }
        fetchAll()
    }

    override fun setMinimumSpacing(airportIcao: String, minimumSpacingDistanceNm: Double): Result<Unit> =
        runCatching {
            throw UnsupportedInSlaveModeException("Minimum spacing cannot be changed in slave mode")
        }

    override fun refreshWeatherData(airportIcao: String, lat: Double, lon: Double): Result<Unit> =
        runCatching {
             throw UnsupportedInSlaveModeException("Weather data update cannot be triggered in slave mode")
        }

    override fun suggestScheduledTime(
        sequenceId: String,
        callsign: String,
        scheduledTime: Instant
    ): Result<Unit> =
        runCatching {
            throw UnsupportedInSlaveModeException("Sequence cannot be changed in slave mode")
        }

    override fun reSchedule(sequenceId: String, callSign: String?): Result<Unit> =
        runCatching {
            throw UnsupportedInSlaveModeException("Sequence cannot be changed in slave mode")
        }

    override fun isTimeSlotAvailable(
        sequenceId: String,
        callsign: String,
        scheduledTime: Instant
    ): Result<Boolean> =
        runCatching {
            throw UnsupportedInSlaveModeException("Sequence cannot be changed in slave mode")
        }

    override fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?> =
        runCatching {
            throw UnsupportedInSlaveModeException("Descent profile cannot be viewed in slave mode")
        }
}