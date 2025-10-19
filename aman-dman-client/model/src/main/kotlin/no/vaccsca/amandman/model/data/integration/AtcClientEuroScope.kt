package no.vaccsca.amandman.model.data.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.core.JsonFactory
import kotlinx.coroutines.*
import no.vaccsca.amandman.model.data.dto.euroscope.ArrivalJson
import no.vaccsca.amandman.model.data.dto.euroscope.ArrivalsUpdateFromServerJson
import no.vaccsca.amandman.model.data.dto.euroscope.AssignRunwayMessage
import no.vaccsca.amandman.model.data.dto.euroscope.ControllerInfoFromServerJson
import no.vaccsca.amandman.model.data.dto.euroscope.DeparturesUpdateFromServerJson
import no.vaccsca.amandman.model.data.dto.euroscope.MessageFromServerJson
import no.vaccsca.amandman.model.data.dto.euroscope.MessageToEuroScopePluginJson
import no.vaccsca.amandman.model.data.dto.euroscope.RegisterFixInboundsMessageJson
import no.vaccsca.amandman.model.data.dto.euroscope.RunwayStatusJson
import no.vaccsca.amandman.model.data.dto.euroscope.RunwayStatusesUpdateFromServerJson
import no.vaccsca.amandman.model.data.dto.euroscope.UnregisterTimelineMessageJson
import no.vaccsca.amandman.model.data.repository.NavdataRepository
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.model.domain.valueobjects.AircraftPosition
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.LatLng
import no.vaccsca.amandman.model.domain.valueobjects.Waypoint
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientRunwaySelectionData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.ControllerInfoData
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.collections.get

class AtcClientEuroScope(
    private val navdataRepository: NavdataRepository,
    private val controllerInfoCallback: ((ControllerInfoData) -> Unit),
    private val host: String = SettingsRepository.getSettings(reload = true).connectionConfig.atcClient.host,
    private val port: Int = SettingsRepository.getSettings(reload = true).connectionConfig.atcClient.port ?: 12345,
) : AtcClient {
    private var isRunning = false
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val arrivalCallbacks = mutableMapOf<String, (List<AtcClientArrivalData>) -> Unit>()
    private val runwayStatusCallbacks = mutableMapOf<String, (List<AtcClientRunwaySelectionData>) -> Unit>()
    private var nextRequestId = 0
        get() {
            return field++
        }

    private val requestIdToAirportMap = mutableMapOf<Int, String>()

    private val objectMapper = jacksonObjectMapper().apply {
        // Configure Jackson for large messages
        factory.configure(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING, true)
    }

    val isClientConnected: Boolean
        get() = isConnected

    override fun start(onControllerInfoData: (ControllerInfoData) -> Unit) {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (true) {
                if (!isConnected) {
                    println("Attempting to connect to $host:$port")
                    try {
                        socket = Socket(host, port)
                        
                        // Configure socket for large messages
                        socket!!.receiveBufferSize = 256 * 1024 // 256KB receive buffer
                        socket!!.sendBufferSize = 64 * 1024    // 64KB send buffer
                        socket!!.tcpNoDelay = true             // Disable Nagle's algorithm for faster small message sending
                        
                        writer = OutputStreamWriter(socket!!.getOutputStream(), Charsets.UTF_8)
                        reader = InputStreamReader(socket!!.getInputStream(), Charsets.UTF_8)
                        isConnected = true
                        println("Connected to $host:$port (buffers: recv=${socket!!.receiveBufferSize}, send=${socket!!.sendBufferSize})")

                        onConnectionEstablished()
                        launch { receiveMessages() }
                    } catch (e: Exception) {
                        println("Connection failed: ${e.message}")
                        delay(1000)
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    override fun collectDataFor(
        airportIcao: String,
        onArrivalsReceived: (List<AtcClientArrivalData>) -> Unit,
        onRunwaySelectionChanged: (List<AtcClientRunwaySelectionData>) -> Unit
    ) {
        arrivalCallbacks[airportIcao] = onArrivalsReceived
        runwayStatusCallbacks[airportIcao] = onRunwaySelectionChanged

        val theRequestId = nextRequestId
        requestIdToAirportMap[theRequestId] = airportIcao
    }

    override fun stopCollectingMovementsFor(airportIcao: String) {
        // Send unregister message to the server
        requestIdToAirportMap.filterValues { it == airportIcao }.keys.forEach { requestId ->
            sendMessage(
                UnregisterTimelineMessageJson(requestId)
            )
        }

        // Clean up local callbacks for this airport
        runwayStatusCallbacks.remove(airportIcao)
        arrivalCallbacks.remove(airportIcao)
    }

    override fun assignRunway(callsign: String, newRunway: String) {
        sendMessage(
            AssignRunwayMessage(
                requestId = nextRequestId,
                callsign = callsign,
                runway = newRunway
            )
        )
    }

    override fun close() {
        try {
            isConnected = false
            isRunning = false
            scope.cancel()

            socket?.close()
            writer?.close()
            reader?.close()
        } catch (e: Exception) {
            println("Error closing connection: ${e.message}")
        } finally {
            socket = null
            writer = null
            reader = null
        }
    }

    private fun onConnectionEstablished() {
        // Resubscribe to all previously registered airports
        val currentAirports = requestIdToAirportMap.values.toSet()
        requestIdToAirportMap.clear()
        currentAirports.forEach { airportIcao ->
            val theRequestId = nextRequestId
            requestIdToAirportMap[theRequestId] = airportIcao

            sendMessage(
                RegisterFixInboundsMessageJson(
                    requestId = theRequestId,
                    targetFixes = emptyList(),
                    viaFixes = emptyList(), // TODO
                    destinationAirports = listOf(airportIcao)
                )
            )
        }
    }

    private fun sendMessage(message: MessageToEuroScopePluginJson) {
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            writer?.write(jsonMessage + "\n")
            writer?.flush()
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
        }
    }

    private suspend fun receiveMessages() {
        try {
            // Use larger buffer for reading large messages (256KB buffer)
            val bufferedReader = BufferedReader(reader, 256 * 1024)
            
            // Set socket timeout for read operations (60 seconds for better stability)
            socket?.soTimeout = 60000

            println("Starting message receive loop...")
            var messageCount = 0
            var lastMessageTime = System.currentTimeMillis()

            while (isConnected) {
                try {
                    val message = bufferedReader.readLine()
                    if (message == null) {
                        println("readLine() returned null - server closed connection")
                        break
                    }
                    
                    messageCount++
                    lastMessageTime = System.currentTimeMillis()

                    try {
                        val dataPackage = objectMapper.readValue(message, MessageFromServerJson::class.java)
                        handleMessage(dataPackage)
                    } catch (e: Exception) {
                        println("Error parsing JSON message #$messageCount (length: ${message.length}): ${e.message}")
                        println("Message start: ${message.take(100)}")
                        e.printStackTrace()
                        // Continue processing other messages even if one fails
                        continue
                    }
                } catch (e: SocketTimeoutException) {
                    val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime
                    println("Socket read timeout after ${timeSinceLastMessage}ms - checking connection...")

                    // If no message for more than 2 minutes, consider connection dead
                    if (timeSinceLastMessage > 120000) {
                        println("No messages received for over 2 minutes - connection may be dead")
                        break
                    }
                    continue
                } catch (e: IOException) {
                    println("IOException while reading: ${e.message}")
                    e.printStackTrace()
                    break
                } catch (e: Exception) {
                    println("Unexpected error while reading: ${e.message}")
                    e.printStackTrace()
                    break
                }
            }
        } catch (e: Exception) {
            println("Error receiving message: ${e.message}")
            e.printStackTrace()
        } finally {
            println("Disconnected from server.")
            close()
            isConnected = false
        }
    }

    private fun handleMessage(messageFromServerJson: MessageFromServerJson) {
        when (messageFromServerJson) {
            is ArrivalsUpdateFromServerJson -> {
                val airportIcao = requestIdToAirportMap[messageFromServerJson.requestId]
                arrivalCallbacks[airportIcao]?.invoke(messageFromServerJson.inbounds.map { it.toArrival() })
            }
            is DeparturesUpdateFromServerJson -> {
                TODO("Departures not yet implemented")
            }
            is RunwayStatusesUpdateFromServerJson -> {
                messageFromServerJson.airports.forEach { (airportIcao, statusesJson) ->
                    val statuses = statusesJson.map { (name, statusJson) -> statusJson.toRunwayStatus(name) }
                    runwayStatusCallbacks[airportIcao]?.invoke(statuses)
                }
            }
            is ControllerInfoFromServerJson -> {
                val infoData = ControllerInfoData(
                    callsign = messageFromServerJson.me.callsign,
                    positionId = messageFromServerJson.me.positionId,
                    facilityType = facilityTypeToString(messageFromServerJson.me.facilityType),
                )
                controllerInfoCallback(infoData)
            }
        }
    }

    private fun ArrivalJson.toArrival(): AtcClientArrivalData {
        return AtcClientArrivalData(
            callsign = this.callsign,
            icaoType = this.icaoType,
            assignedRunway = this.assignedRunway,
            assignedStar = this.assignedStar,
            assignedDirect = this.assignedDirect,
            scratchPad = this.scratchPad,
            remainingWaypoints = this.route.filter { !it.isPassed }.map {
                Waypoint(id = it.name, latLng = LatLng(it.latitude, it.longitude))
            },
            currentPosition = AircraftPosition(
                latLng = LatLng(this.latitude, this.longitude),
                altitudeFt = this.pressureAltitude,
                flightLevel = this.flightLevel,
                groundspeedKts = this.groundSpeed,
                trackDeg = this.track,
            ),
            arrivalAirportIcao = this.arrivalAirportIcao,
            flightPlanTas = this.flightPlanTas,
            trackingController = this.trackingController
        )
    }

    private fun RunwayStatusJson.toRunwayStatus(name: String) =
        AtcClientRunwaySelectionData(
            runway = name,
            allowArrivals = this.arrivals,
            allowDepartures = this.departures,
        )

    private fun facilityTypeToString(facilityType: Int?): String {
        return when (facilityType) {
            1 -> "FSS"
            2 -> "DEL"
            3 -> "GND"
            4 -> "TWR"
            5 -> "APP"
            6 -> "CTR"
            else -> "UNKNOWN"
        }
    }
}