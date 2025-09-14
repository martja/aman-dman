package no.vaccsca.amandman.model.data.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.service.DataUpdateListener
import no.vaccsca.amandman.model.domain.valueobjects.SequenceStatus
import no.vaccsca.amandman.model.domain.valueobjects.TrajectoryPoint
import no.vaccsca.amandman.model.data.repository.AircraftPerformanceData
import no.vaccsca.amandman.model.domain.service.DescentTrajectoryService
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.data.repository.NavdataRepository
import no.vaccsca.amandman.model.data.repository.WeatherDataRepository
import no.vaccsca.amandman.model.data.service.integration.AtcClient
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.collections.plus

/**
 * This is only used in the Master and Local instances of the application
 */
class PlannerServiceMaster(
    private val weatherDataRepository: WeatherDataRepository,
    private val navdataRepository: NavdataRepository,
    private val atcClient: AtcClient,
    private vararg val dataUpdateListeners: DataUpdateListener,
) : PlannerService {
    private var weatherData: VerticalWeatherProfile? = null

    // ID of the sequence, typically the airport ICAO code
    private val arrivalsCache: MutableMap<String, List<RunwayArrivalEvent>> = mutableMapOf()
    private val sequences: MutableMap<String, Sequence> = mutableMapOf()
    private var minimumSpacingNm = 3.0 // Minimum spacing in nautical miles

    private val descentTrajectoryCache = mutableMapOf<String, List<TrajectoryPoint>>()

    init {
        refreshWeatherData("ENGM", 60.186122, 11.098964)
    }

    override fun planArrivalsFor(
        airportIcao: String,
    ) {
        if (!sequences.containsKey(airportIcao)) {
            sequences[airportIcao] = Sequence(mutableListOf())
        }
        atcClient.collectMovementsFor(airportIcao,
            onDataReceived = { arrivals ->
                handleUpdateFromAtcClient(airportIcao, arrivals)
            },
            onRunwaySelectionChanged = { runways ->
                val map = runways.associate {
                    it.runway to RunwayStatus(it.allowArrivals,it.allowDepartures)
                }
                dataUpdateListeners.forEach {
                    it.onRunwayModesUpdated(airportIcao, map)
                }
            }
        )
    }

    private fun handleUpdateFromAtcClient(icao: String, arrivals: List<AtcClientArrivalData>) {
        val runwayArrivalEvents = arrivals.mapNotNull { arrival -> createRunwayArrivalEvent(arrival) }
        val sequenceItems = runwayArrivalEvents.map {
            AircraftSequenceCandidate(
                callsign = it.callsign,
                preferredTime = it.estimatedTime,
                landingIas = it.landingIas,
                wakeCategory = it.wakeCategory
            )
        }

        val aircraftToRemove = sequences[icao]!!.sequecencePlaces.map { it.item.id }.filter { it !in runwayArrivalEvents.map { it.callsign } }
        val cleanedSequence = AmanDmanSequenceService.removeFromSequence(sequences[icao]!!, *aircraftToRemove.toTypedArray())
        sequences[icao] = AmanDmanSequenceService.updateSequence(cleanedSequence, sequenceItems, minimumSpacingNm)
        arrivalsCache[icao] = runwayArrivalEvents.map { arrivalEvent ->
            val sequenceSchedule = sequences[icao]!!.sequecencePlaces.find { it.item.id == arrivalEvent.callsign }?.scheduledTime
            arrivalEvent.copy(
                scheduledTime = sequenceSchedule ?: arrivalEvent.scheduledTime,
                sequenceStatus = if (sequenceSchedule != null) SequenceStatus.OK else SequenceStatus.AWAITING_FOR_SEQUENCE,
            )
        }
        onSequenceUpdated(icao)
    }

    private fun createRunwayArrivalEvent(arrival: AtcClientArrivalData): RunwayArrivalEvent? {
        val aircraftPerformance = try {
            AircraftPerformanceData.get(arrival.icaoType)
        } catch (e: Exception) {
            println("Error fetching performance data for ${arrival.icaoType}: ${e.message}")
            return null
        }

        val star = navdataRepository.stars.find { it.id == arrival.assignedStar && it.runway == arrival.assignedRunway }

        val trajectory = DescentTrajectoryService.calculateDescentTrajectory(
            route = arrival.route,
            aircraftPosition = arrival.position,
            verticalWeatherProfile = weatherData,
            star = star,
            aircraftPerformance = aircraftPerformance,
            flightPlanTas = arrival.flightPlanTas,
        )

        descentTrajectoryCache[arrival.callsign] = trajectory

        if (arrival.assignedRunway == null) {
            return null
        }

        val estimatedTime = Clock.System.now() + trajectory.first().remainingTime

        return RunwayArrivalEvent(
            callsign = arrival.callsign,
            icaoType = arrival.icaoType,
            flightLevel = arrival.position.flightLevel,
            groundSpeed = arrival.position.groundspeedKts,
            wakeCategory = aircraftPerformance.takeOffWTC,
            assignedStar = arrival.assignedStar,
            trackingController = arrival.trackingController,
            runway = arrival.assignedRunway,
            estimatedTime = estimatedTime,
            scheduledTime = estimatedTime,
            pressureAltitude = arrival.position.altitudeFt,
            airportIcao = arrival.arrivalAirportIcao,
            remainingDistance = trajectory.first().remainingDistance,
            timelineId = 0,
            assignedStarOk = star != null,
            withinActiveAdvisoryHorizon = false,
            sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE,
            landingIas = aircraftPerformance.landingVat
        )
    }

    override fun setMinimumSpacing(airportIcao: String, minimumSpacingDistanceNm: Double): Result<Unit> =
        runCatching {
            // TODO: use airportIcao
            this.minimumSpacingNm = minimumSpacingDistanceNm
            sequences.forEach { (sequenceId, sequence) ->
                sequences[sequenceId] = AmanDmanSequenceService.reSchedule(sequence)
                onSequenceUpdated(sequenceId)
                // Notify listeners of the spacing change
                dataUpdateListeners.forEach { listener ->
                    listener.onMinimumSpacingUpdated(sequenceId, minimumSpacingDistanceNm)
                }
            }
        }

    override fun refreshWeatherData(airportIcao: String, lat: Double, lon: Double): Result<Unit> =
        runCatching {
            Thread {
                val weather = weatherDataRepository.getWindData(lat, lon)
                weatherData = weather
                dataUpdateListeners.forEach { listener ->
                    listener.onWeatherDataUpdated(airportIcao, weather)
                }
            }.start()
        }

    override fun suggestScheduledTime(sequenceId: String, callsign: String, scheduledTime: Instant): Result<Unit> =
        runCatching {
            if (checkTimeSlotAvailable(sequenceId, callsign, scheduledTime)) {
                sequences[sequenceId] = AmanDmanSequenceService.suggestScheduledTime(sequences[sequenceId]!!, callsign, scheduledTime, minimumSpacingNm)
                onSequenceUpdated(sequenceId)
            } else {
                println("Time slot is not available for $callsign at $scheduledTime")
            }
        }

    override fun reSchedule(sequenceId: String, callSign: String?): Result<Unit> =
        runCatching {
            if (callSign == null) {
                sequences[sequenceId] = AmanDmanSequenceService.reSchedule(sequences[sequenceId]!!)
            } else {
                sequences[sequenceId] = AmanDmanSequenceService.removeFromSequence(sequences[sequenceId]!!, callSign)
            }
            onSequenceUpdated(sequenceId)
        }

    override fun isTimeSlotAvailable(
        sequenceId: String,
        callsign: String,
        scheduledTime: Instant
    ): Result<Boolean> =
        runCatching {
            checkTimeSlotAvailable(sequenceId, callsign, scheduledTime)
        }

    private fun checkTimeSlotAvailable(
        sequenceId: String,
        callsign: String,
        scheduledTime: Instant
    ): Boolean {
        return AmanDmanSequenceService.isTimeSlotAvailable(sequences[sequenceId]!!, callsign, scheduledTime)
    }

    override  fun getDescentProfileForCallsign(callsign: String): Result<List<TrajectoryPoint>?> =
        runCatching {
            descentTrajectoryCache[callsign]
        }

    /**
     * Refreshes the UI with the current sequence data by updating the latest arrivals
     * with the current sequence information and notifying the UI through the live data interface
     */
    private fun onSequenceUpdated(sequenceId: String) {
        val sequence = sequences[sequenceId] ?: return
        val latestArrivals = arrivalsCache[sequenceId] ?: return

        val updatedArrivals = latestArrivals
            .map { arrivalEvent -> arrivalEvent.updateScheduledTime(sequence) }
            .sortedByDescending { it.scheduledTime }

        val sequencedArrivals = if (updatedArrivals.size <= 1) {
            // Handle single or empty list - no zipWithNext needed
            updatedArrivals.map { it.withDistanceToPreceding(null) }
        } else {
            // Process pairs and add the last element
            val pairedArrivals = updatedArrivals.zipWithNext { a, b -> a.withDistanceToPreceding(b) }
            val lastArrival = updatedArrivals.last().withDistanceToPreceding(null)
            (pairedArrivals + lastArrival).reversed()
        }

        arrivalsCache[sequenceId] = sequencedArrivals
        dataUpdateListeners.forEach { listener ->
            listener.onLiveData(sequenceId, sequencedArrivals)
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

    /**
     * Calculates the distance to the preceding aircraft based on the
     * descent trajectory which included the remaining track miles
     */
    private fun RunwayArrivalEvent.withDistanceToPreceding(next: RunwayArrivalEvent?): RunwayArrivalEvent {
        val distanceToPreceding = if (next != null) {
            this.remainingDistance - next.remainingDistance
        } else {
            this.remainingDistance
        }
        return this.copy(distanceToPreceding = distanceToPreceding)
    }

}