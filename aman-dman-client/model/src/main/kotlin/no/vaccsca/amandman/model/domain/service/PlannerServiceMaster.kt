package no.vaccsca.amandman.model.domain.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.data.repository.AircraftPerformanceData
import no.vaccsca.amandman.model.data.repository.WeatherDataRepository
import no.vaccsca.amandman.model.data.integration.AtcClient
import no.vaccsca.amandman.model.domain.valueobjects.Airport
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.time.Duration.Companion.seconds

/**
 * This is only used in the Master and Local instances of the application
 */
class PlannerServiceMaster(
    private val airport: Airport,
    private val weatherDataRepository: WeatherDataRepository,
    private val atcClient: AtcClient,
    private vararg val dataUpdateListeners: DataUpdateListener,
) : PlannerService(airport.icao) {
    private var weatherData: VerticalWeatherProfile? = null

    // ID of the sequence, typically the airport ICAO code
    private var arrivalsCache: List<RunwayArrivalEvent> = emptyList()
    private var sequence: Sequence = Sequence(emptyList())
    private var minimumSpacingNm = 3.0 // Minimum spacing in nautical miles

    private val descentTrajectoryCache = mutableMapOf<String, List<TrajectoryPoint>>()

    init {
        refreshWeatherData()
    }

    override fun stop() {
        atcClient.stopCollectingMovementsFor(airportIcao)
    }

    override fun start() {
        atcClient.start()
    }

    override fun planArrivals() {
        atcClient.collectDataFor(airportIcao,
            onArrivalsReceived = { arrivals ->
                handleUpdateFromAtcClient(arrivals)
            },
            onRunwaySelectionChanged = { runways ->
                val map = runways.associate {
                    it.runway to RunwayStatus(it.allowArrivals, it.allowDepartures)
                }
                dataUpdateListeners.forEach {
                    it.onRunwayModesUpdated(airportIcao, map)
                }
            }
        )
    }

    private fun handleUpdateFromAtcClient(arrivals: List<AtcClientArrivalData>) {
        val runwayArrivalEvents = arrivals.mapNotNull { arrival -> createRunwayArrivalEvent(arrival) }
        val sequenceItems = runwayArrivalEvents.map {
            AircraftSequenceCandidate(
                callsign = it.callsign,
                preferredTime = it.estimatedTime,
                landingIas = it.landingIas,
                wakeCategory = it.wakeCategory
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

    private fun createRunwayArrivalEvent(arrival: AtcClientArrivalData): RunwayArrivalEvent? {
        val aircraftPerformance = try {
            AircraftPerformanceData.get(arrival.icaoType)
        } catch (e: Exception) {
            println("Error fetching performance data for ${arrival.icaoType}: ${e.message}")
            return null
        }

        if (arrival.assignedRunway == null) {
            println("No runway assigned for ${arrival.callsign}, skipping arrival event creation.")
            return null
        }

        val star = airport.stars.find { it.id == arrival.assignedStar }

        val trajectory = DescentTrajectoryService.calculateDescentTrajectory(
            currentPosition = arrival.currentPosition,
            assignedRunway = arrival.assignedRunway,
            remainingWaypoints = arrival.remainingWaypoints,
            verticalWeatherProfile = weatherData,
            star = star,
            aircraftPerformance = aircraftPerformance,
            flightPlanTas = arrival.flightPlanTas,
            arrivalAirportIcao = arrival.arrivalAirportIcao
        )

        if (trajectory.isEmpty()) {
            println("No trajectory for ${arrival.callsign}, skipping arrival event creation.")
            return null
        }

        descentTrajectoryCache[arrival.callsign] = trajectory

        val estimatedTime = Clock.System.now() + (trajectory.firstOrNull()?.remainingTime ?: 0.seconds)

        return RunwayArrivalEvent(
            callsign = arrival.callsign,
            icaoType = arrival.icaoType,
            flightLevel = arrival.currentPosition.flightLevel,
            groundSpeed = arrival.currentPosition.groundspeedKts,
            wakeCategory = aircraftPerformance.takeOffWTC,
            assignedStar = arrival.assignedStar,
            trackingController = arrival.trackingController,
            runway = arrival.assignedRunway,
            estimatedTime = estimatedTime,
            scheduledTime = estimatedTime,
            pressureAltitude = arrival.currentPosition.altitudeFt,
            airportIcao = arrival.arrivalAirportIcao,
            remainingDistance = trajectory.first().remainingDistance,
            timelineId = 0,
            assignedStarOk = star != null,
            withinActiveAdvisoryHorizon = false,
            sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE,
            landingIas = aircraftPerformance.landingVat
        )
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

    override fun suggestScheduledTime(callsign: String, scheduledTime: Instant): Result<Unit> =
        runCatching {
            if (checkTimeSlotAvailable(callsign, scheduledTime)) {
                sequence = AmanDmanSequenceService.suggestScheduledTime(sequence, callsign, scheduledTime, minimumSpacingNm)
                onSequenceUpdated()
            } else {
                println("Time slot is not available for $callsign at $scheduledTime")
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
        callsign: String,
        scheduledTime: Instant
    ): Result<Boolean> =
        runCatching {
            checkTimeSlotAvailable(callsign, scheduledTime)
        }

    private fun checkTimeSlotAvailable(
        callsign: String,
        scheduledTime: Instant
    ): Boolean {
        return AmanDmanSequenceService.isTimeSlotAvailable(sequence, callsign, scheduledTime)
    }

    override  fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?> =
        runCatching {
            descentTrajectoryCache[callsign]
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
                a.copy(distanceToPreceding = a.remainingDistance - b.remainingDistance)
            }
            (pairedArrivals + updatedArrivals.last()).reversed()
        }

        arrivalsCache = sequencedArrivals
        dataUpdateListeners.forEach { listener ->
            listener.onLiveData(airportIcao, sequencedArrivals)
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