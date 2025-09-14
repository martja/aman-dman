package no.vaccsca.amandman.model.data.service.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.core.JsonFactory
import kotlinx.coroutines.*
import no.vaccsca.amandman.model.data.dto.atcClientMessage.ArrivalJson
import no.vaccsca.amandman.model.data.dto.atcClientMessage.ArrivalsUpdateJson
import no.vaccsca.amandman.model.data.dto.atcClientMessage.DeparturesUpdateJson
import no.vaccsca.amandman.model.data.dto.atcClientMessage.IncomingMessageJson
import no.vaccsca.amandman.model.data.dto.atcClientMessage.MessageToServer
import no.vaccsca.amandman.model.data.dto.atcClientMessage.RegisterFixInboundsMessage
import no.vaccsca.amandman.model.data.dto.atcClientMessage.RunwayStatusJson
import no.vaccsca.amandman.model.data.dto.atcClientMessage.RunwayStatusesUpdateJson
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.model.domain.valueobjects.AircraftPosition
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.LatLng
import no.vaccsca.amandman.model.domain.valueobjects.RoutePoint
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientRunwaySelectionData
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

class AtcClientEuroScope(
    private val host: String = SettingsRepository.getSettings(reload = true).connectionConfig.atcClient.host,
    private val port: Int = SettingsRepository.getSettings(reload = true).connectionConfig.atcClient.port ?: 12345,
) : AtcClient {
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null

    private val objectMapper = jacksonObjectMapper().apply {
        // Configure Jackson for large messages
        factory.configure(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING, true)
    }
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nextRequestId = 0
        get() {
            return field++
        }

    private val arrivalCallbacks = mutableMapOf<Int, (List<AtcClientArrivalData>) -> Unit>()
    private val runwayStatusCallbacks = mutableMapOf<String, (List<AtcClientRunwaySelectionData>) -> Unit>()

    init {
        startConnectionLoop()
    }

    private fun startConnectionLoop() {
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

    override fun collectMovementsFor(
        airportIcao: String,
        onDataReceived: (List<AtcClientArrivalData>) -> Unit,
        onRunwaySelectionChanged: (List<AtcClientRunwaySelectionData>) -> Unit
    ) {
        val timelineId = nextRequestId
        arrivalCallbacks[timelineId] = onDataReceived
        runwayStatusCallbacks[airportIcao] = onRunwaySelectionChanged
        sendMessage(
            RegisterFixInboundsMessage(
                requestId = timelineId,
                targetFixes = emptyList(),
                viaFixes = emptyList(), // TODO
                destinationAirports = listOf(airportIcao)
            )
        )
    }

    private fun sendMessage(message: MessageToServer) {
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
                        val dataPackage = objectMapper.readValue(message, IncomingMessageJson::class.java)
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

    private fun handleMessage(incomingMessageJson: IncomingMessageJson) {
        when (incomingMessageJson) {
            is ArrivalsUpdateJson -> {
                arrivalCallbacks[incomingMessageJson.requestId]?.invoke(incomingMessageJson.inbounds.map { it.toArrival() })
            }
            is DeparturesUpdateJson -> {
                TODO("Departures not yet implemented")
            }
            is RunwayStatusesUpdateJson -> {
                incomingMessageJson.airports.forEach { (airportIcao, statusesJson) ->
                    val statuses = statusesJson.map { (name, statusJson) -> statusJson.toRunwayStatus(name) }
                    runwayStatusCallbacks[airportIcao]?.invoke(statuses)
                }
            }
        }
    }

    private fun ArrivalJson.toArrival() =
        AtcClientArrivalData(
            callsign = this.callsign,
            icaoType = this.icaoType,
            position = AircraftPosition(
                position = LatLng(this.latitude, this.longitude),
                altitudeFt = this.pressureAltitude,
                flightLevel = this.flightLevel,
                groundspeedKts = this.groundSpeed,
                trackDeg = this.track,
            ),
            assignedRunway = this.assignedRunway,
            assignedStar = this.assignedStar,
            assignedDirect = this.assignedDirect,
            scratchPad = this.scratchPad,
            track = this.track,
            route = this.route.map {
                RoutePoint(
                    id = it.name,
                    position = LatLng(it.latitude, it.longitude),
                    isOnStar = it.isOnStar,
                    isPassed = it.isPassed
                )
            },
            arrivalAirportIcao = this.arrivalAirportIcao,
            flightPlanTas = this.flightPlanTas,
            trackingController = this.trackingController
        )

    private fun RunwayStatusJson.toRunwayStatus(name: String) =
        AtcClientRunwaySelectionData(
            runway = name,
            allowArrivals = this.departures,
            allowDepartures = this.arrivals,
        )

    fun close() {
        try {
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
}