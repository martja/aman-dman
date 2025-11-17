package no.vaccsca.amandman.model.domain.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.model.data.repository.WeatherDataRepository
import no.vaccsca.amandman.model.data.integration.AtcClient
import no.vaccsca.amandman.model.data.repository.CdmClient
import no.vaccsca.amandman.model.domain.exception.DescentTrajectoryException
import no.vaccsca.amandman.model.domain.valueobjects.Airport
import no.vaccsca.amandman.model.domain.valueobjects.CdmData
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientDepartureData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.ControllerInfoData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.time.Duration.Companion.seconds

/**
 * This is only used in the Master and Local instances of the application
 */
class PlannerServiceMaster(
    private val airport: Airport,
    private val weatherDataRepository: WeatherDataRepository,
    private val atcClient: AtcClient,
    private val cdmClient: CdmClient,
    private vararg val dataUpdateListeners: DataUpdateListener,
) : PlannerService(airport.icao) {
    private var weatherData: VerticalWeatherProfile? = null
    private var cdmDepartures: List<CdmData>? = null

    // ID of the sequence, typically the airport ICAO code
    private var arrivalsCache: List<RunwayArrivalEvent> = emptyList()
    private var departuresCache: List<DepartureEvent> = emptyList()
    private var sequence: Sequence = Sequence(emptyList())
    private var minimumSpacingNm = 3.0 // Minimum spacing in nautical miles
    private var availableRunways: List<String>? = null
    private var controllerInfo: ControllerInfoData? = null
    private var fetchCdmData = false

    init {
        refreshWeatherData()

        // Periodic refresh of CDM data
        Thread {
            while (true) {
                Thread.sleep(2_000)
                if (fetchCdmData) {
                    println("Refreshing CDM data for $airportIcao")
                    refreshCdmData()
                }
            }
        }.start()

        // Periodic cleanup of old cached flights
        Thread {
            while (true) {
                Thread.sleep(1_000) // check every second
                val cutoff = NtpClock.now().minus(5.seconds)
                arrivalsCache = arrivalsCache.filter { it.lastTimestamp >= cutoff }
                departuresCache = departuresCache.filter { it.lastTimestamp >= cutoff }
                onSequenceUpdated()
            }
        }.start()
    }

    override fun stop() {
        atcClient.stopCollectingMovementsFor(airportIcao)
    }

    override fun start() {
        atcClient.start(
            onControllerInfoData = {
                controllerInfo = it
            }
        )
    }

    override fun getAvailableRunways(): Result<List<String>> {
        return Result.success(availableRunways ?: emptyList())
    }

    override fun setShowDepartures(showDepartures: Boolean) {
        this.fetchCdmData = showDepartures
        if (showDepartures) {
            refreshCdmData()
        } else {
            this.cdmDepartures = emptyList()
            this.departuresCache = emptyList()
            onSequenceUpdated()
        }
    }

    override fun planArrivals() {
        atcClient.collectDataFor(airportIcao,
            onArrivalsReceived = { arrivals ->
                handleArrivalsUpdateFromAtcClient(arrivals)
            },
            onDeparturesReceived = { departures ->
                handleDeparturesUpdateFromAtcClient(departures)
            },
            onRunwaySelectionChanged = { runways ->
                val map = runways.associate {
                    it.runway to RunwayStatus(it.allowArrivals, it.allowDepartures)
                }
                dataUpdateListeners.forEach {
                    it.onRunwayModesUpdated(airportIcao, map)
                }
                availableRunways = runways.map { it.runway }
            },
        )
    }

    private fun handleArrivalsUpdateFromAtcClient(arrivals: List<AtcClientArrivalData>) {
        val runwayArrivalEvents = makeRunwayArrivalEvents(arrivals)
        val sequenceItems = runwayArrivalEvents.map {
            AircraftSequenceCandidate(
                callsign = it.callsign,
                preferredTime = it.estimatedTime,
                landingIas = it.landingIas,
                wakeCategory = it.wakeCategory,
                assignedRunway = it.runway
            )
        }

        val aircraftToRemove = sequence.sequecencePlaces.map { it.item.id }.filter { it !in runwayArrivalEvents.map { it.callsign } }
        val cleanedSequence = AmanDmanSequenceService.removeFromSequence(sequence, *aircraftToRemove.toTypedArray())
        sequence = AmanDmanSequenceService.updateSequence(cleanedSequence, sequenceItems, minimumSpacingNm)
        arrivalsCache = runwayArrivalEvents.map { arrivalEvent ->
            val sequenceSchedule = sequence.sequecencePlaces.find { it.item.id == arrivalEvent.callsign }?.scheduledTime
            arrivalEvent.copy(
                scheduledTime = sequenceSchedule ?: arrivalEvent.scheduledTime,
                sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
            )
        }
        onSequenceUpdated()
    }

    private fun handleDeparturesUpdateFromAtcClient(departures: List<AtcClientDepartureData>) {
        departuresCache = makeDepartureEvents(departures)
        onSequenceUpdated()
    }

    private fun makeRunwayArrivalEvents(arrivals: List<AtcClientArrivalData>): List<RunwayArrivalEvent> =
        arrivals.mapNotNull { arrival ->
            try {
                ArrivalEventService.createRunwayArrivalEvent(airport, arrival, weatherData)
            } catch (e: DescentTrajectoryException) {
                println("Failed to map arrival ${arrival.callsign}: ${e.message}")
                null
            }
        }

    private fun makeDepartureEvents(departures: List<AtcClientDepartureData>): List<DepartureEvent> =
        departures.mapNotNull { departure ->
            try {
                DepartureEventService.createRunwayDepartureEvent(departure, cdmDepartures)
            } catch (e: Exception) {
                println("Failed to map departure ${departure.callsign}: ${e.message}")
                null
            }
        }

    override fun setMinimumSpacing(minimumSpacingDistanceNm: Double): Result<Unit> =
        runCatching {
            // TODO: use airportIcao
            this.minimumSpacingNm = minimumSpacingDistanceNm
            sequence = AmanDmanSequenceService.reSchedule(sequence)
            onSequenceUpdated()
            // Notify listeners of the spacing change
            dataUpdateListeners.forEach { listener ->
                listener.onMinimumSpacingUpdated(airportIcao, minimumSpacingDistanceNm)
            }
        }

    override fun refreshWeatherData(): Result<Unit> =
        runCatching {
            Thread {
                val weather = weatherDataRepository.getWindData(airport.location.lat, airport.location.lon)
                weatherData = weather
                dataUpdateListeners.forEach { listener ->
                    listener.onWeatherDataUpdated(airportIcao, weather)
                }
            }.start()
        }

    override fun refreshCdmData(): Result<Unit> =
        runCatching {
            Thread {
                this.cdmDepartures = cdmClient.fetchCdmDepartures(airportIcao)
            }.start()
        }

    override fun suggestScheduledTime(timelineEvent: TimelineEvent, scheduledTime: Instant, newRunway: String?): Result<Unit> =
        runCatching {
            if (timelineEvent !is RunwayArrivalEvent) {
                throw IllegalArgumentException("Only RunwayArrivalEvent is supported at the moment")
            }
            if (checkTimeSlotAvailable(timelineEvent, scheduledTime)) {
                sequence = AmanDmanSequenceService.suggestScheduledTime(sequence, timelineEvent.callsign, scheduledTime, minimumSpacingNm)
                if (newRunway != null) {
                    atcClient.assignRunway(timelineEvent.callsign, newRunway)
                }
                onSequenceUpdated()
            } else {
                println("Time slot is not available for ${timelineEvent.callsign} at $scheduledTime")
            }
        }

    override fun reSchedule(callSign: String?): Result<Unit> =
        runCatching {
            if (callSign == null) {
                sequence = AmanDmanSequenceService.reSchedule(sequence)
            } else {
                sequence = AmanDmanSequenceService.removeFromSequence(sequence, callSign)
            }
            onSequenceUpdated()
        }

    override fun isTimeSlotAvailable(
        timelineEvent: TimelineEvent,
        scheduledTime: Instant
    ): Result<Boolean> =
        runCatching {
            checkTimeSlotAvailable(timelineEvent, scheduledTime)
        }

    private fun checkTimeSlotAvailable(
        timelineEvent: TimelineEvent,
        scheduledTime: Instant
    ): Boolean {
        return AmanDmanSequenceService.isTimeSlotAvailable(sequence, timelineEvent, scheduledTime)
    }

    override  fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?> =
        runCatching {
            ArrivalEventService.getDescentProfileForCallsign(callsign)
        }

    /**
     * Refreshes the UI with the current sequence data by updating the latest arrivals
     * with the current sequence information and notifying the UI through the live data interface
     */
    private fun onSequenceUpdated() {
        val updatedArrivals = arrivalsCache
            .map { arrivalEvent -> arrivalEvent.updateScheduledTime(sequence) }
            .sortedByDescending { it.scheduledTime }

        val sequencedArrivals = if (updatedArrivals.size <= 1) {
            // Handle single or empty list - no zipWithNext needed
            updatedArrivals
        } else {
            // Process pairs and add the last element
            val pairedArrivals = updatedArrivals.zipWithNext { a, b ->
                a.copy(
                    distanceToPreceding = a.remainingDistance - b.remainingDistance,
                    timeToPreceding = a.estimatedTime - b.estimatedTime,
                )
            }
            (pairedArrivals + updatedArrivals.last()).reversed()
        }

        arrivalsCache = sequencedArrivals
        dataUpdateListeners.forEach { listener ->
            listener.onLiveData(airportIcao, sequencedArrivals + departuresCache)
        }
    }

    /**
     * Updates the scheduled time of the RunwayArrivalEvent based on the sequence information.
     * If the sequence has a scheduled time for the callsign, it updates the event's scheduled time
     * and sets the sequence status accordingly.
     */
    private fun RunwayArrivalEvent.updateScheduledTime(sequence: Sequence): RunwayArrivalEvent {
        val sequenceSchedule = sequence.sequecencePlaces.find { it.item.id == this.callsign }?.scheduledTime
        return this.copy(
            scheduledTime = sequenceSchedule ?: this.scheduledTime,
            sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
        )
    }
}