package no.vaccsca.amandman.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.integration.NavdataRepository
import no.vaccsca.amandman.integration.amanConfig.AircraftPerformanceData
import no.vaccsca.amandman.integration.atcClient.AtcClient
import no.vaccsca.amandman.integration.atcClient.entities.ArrivalJson
import no.vaccsca.amandman.integration.weather.WeatherDataRepository
import no.vaccsca.amandman.model.SequenceStatus
import no.vaccsca.amandman.model.TrajectoryPoint
import no.vaccsca.amandman.model.navigation.AircraftPosition
import no.vaccsca.amandman.model.navigation.LatLng
import no.vaccsca.amandman.model.navigation.RoutePoint
import no.vaccsca.amandman.model.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.weather.VerticalWeatherProfile
import no.vaccsca.amandman.service.DescentTrajectoryService.calculateDescentTrajectory

/**
 * This is only used in the Master instance of the application
 */

class AmanPlannerService(
    private val navdataRepository: NavdataRepository,
    private val atcClient: AtcClient,
    private val weatherDataRepository: WeatherDataRepository,
    private vararg val dataUpdatesHandlers: DataUpdatesHandler,
) {
    private var weatherData: VerticalWeatherProfile? = null

    // ID of the sequence, typically the airport ICAO code
    private val arrivalsCache: MutableMap<String, List<RunwayArrivalEvent>> = mutableMapOf()
    private val sequences: MutableMap<String, Sequence> = mutableMapOf()
    private var minimumSpacingNm = 3.0 // Minimum spacing in nautical miles

    private val descentTrajectoryCache = mutableMapOf<String, List<TrajectoryPoint>>()

    fun subscribeForInbounds(icao: String) {
        sequences.computeIfAbsent(icao) { Sequence(emptyList()) }
        arrivalsCache.computeIfAbsent(icao) { emptyList() }

        atcClient.collectArrivalsFor(
            airportIcao =  icao,
            onRunwayModesChanged = { runwayStatuses ->
                dataUpdatesHandlers.forEach { handler ->
                    handler.handleRunwayStatusUpdate(runwayStatuses)
                }
            },
            onDataReceived = { arrivals -> handleArrivalsUpdate(icao, arrivals) }
        )
    }

    private fun handleArrivalsUpdate(icao: String, arrivals: List<ArrivalJson>) {
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

    private fun createRunwayArrivalEvent(arrivalJson: ArrivalJson): RunwayArrivalEvent? {
        val aircraftPerformance = try {
            AircraftPerformanceData.get(arrivalJson.icaoType)
        } catch (e: Exception) {
            println("Error fetching performance data for ${arrivalJson.icaoType}: ${e.message}")
            return null
        }

        val position =
            AircraftPosition(
                position = LatLng(arrivalJson.latitude, arrivalJson.longitude),
                altitudeFt = arrivalJson.flightLevel,
                groundspeedKts = arrivalJson.groundSpeed,
                trackDeg = arrivalJson.track
            )

        val route = arrivalJson.route.map { RoutePoint(it.name, LatLng(it.latitude, it.longitude), it.isPassed, it.isOnStar) }
        val star = navdataRepository.stars.find { it.id == arrivalJson.assignedStar && it.runway == arrivalJson.assignedRunway }

        val trajectory = calculateDescentTrajectory(
            route = route,
            aircraftPosition = position,
            verticalWeatherProfile = weatherData,
            star = star,
            aircraftPerformance = aircraftPerformance,
            flightPlanTas = arrivalJson.flightPlanTas,
        )

        descentTrajectoryCache[arrivalJson.callsign] = trajectory

        if (arrivalJson.assignedRunway == null) {
            return null
        }

        val estimatedTime = Clock.System.now() + trajectory.first().remainingTime

        return RunwayArrivalEvent(
            callsign = arrivalJson.callsign,
            icaoType = arrivalJson.icaoType,
            flightLevel = arrivalJson.flightLevel,
            groundSpeed = arrivalJson.groundSpeed,
            wakeCategory = aircraftPerformance.takeOffWTC,
            assignedStar = arrivalJson.assignedStar,
            trackingController = arrivalJson.trackingController,
            runway = arrivalJson.assignedRunway!!,
            estimatedTime = estimatedTime,
            scheduledTime = estimatedTime,
            pressureAltitude = arrivalJson.pressureAltitude,
            airportIcao = arrivalJson.arrivalAirportIcao,
            remainingDistance = trajectory.first().remainingDistance,
            timelineId = 0,
            assignedStarOk = star != null,
            withinActiveAdvisoryHorizon = false,
            sequenceStatus = SequenceStatus.AWAITING_FOR_SEQUENCE,
            landingIas = aircraftPerformance.landingVat
        )
    }

    fun setMinimumSpacing(newSpacing: Double) {
        this.minimumSpacingNm = newSpacing
        sequences.forEach { (sequenceId, sequence) ->
            sequences[sequenceId] = AmanDmanSequenceService.reSchedule(sequence)
            onSequenceUpdated(sequenceId)
        }
        // Notify controller of the spacing change so UI can be updated
        dataUpdatesHandlers.forEach { handler -> handler.onMinimumSpacingUpdated(newSpacing) }
    }

    fun refreshWeatherData(lat: Double, lon: Double) {
        Thread {
            val weather = weatherDataRepository.getWindData(lat, lon)
            weatherData = weather
            dataUpdatesHandlers.forEach { handler -> handler.onWeatherDataUpdated(weather) }
        }.start()
    }

    fun suggestScheduledTime(sequenceId: String, callsign: String, scheduledTime: Instant) {
        if (isTimeSlotAvailable(sequenceId, callsign, scheduledTime)) {
            sequences[sequenceId] = AmanDmanSequenceService.suggestScheduledTime(sequences[sequenceId]!!, callsign, scheduledTime, minimumSpacingNm)
            onSequenceUpdated(sequenceId)
        } else {
            println("Time slot is not available for $callsign at $scheduledTime")
        }
    }

    fun reSchedule(sequenceId: String, callSign: String?) {
        if (callSign == null) {
            sequences[sequenceId] = AmanDmanSequenceService.reSchedule(sequences[sequenceId]!!)
        } else {
            sequences[sequenceId] = AmanDmanSequenceService.removeFromSequence(sequences[sequenceId]!!, callSign)
        }
        onSequenceUpdated(sequenceId)
    }

    fun isTimeSlotAvailable(
        sequenceId: String,
        callsign: String,
        scheduledTime: Instant
    ): Boolean {
        return AmanDmanSequenceService.isTimeSlotAvailable(sequences[sequenceId]!!, callsign, scheduledTime)
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
        dataUpdatesHandlers.forEach { handler -> handler.onSequenceUpdated(sequenceId, sequencedArrivals) }
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

    fun getDescentProfileForCallsign(callsign: String): List<TrajectoryPoint>? {
        return descentTrajectoryCache[callsign]
    }
}