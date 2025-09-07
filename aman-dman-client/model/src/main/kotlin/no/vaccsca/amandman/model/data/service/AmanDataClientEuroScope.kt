package no.vaccsca.amandman.model.data.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.core.JsonFactory
import kotlinx.coroutines.*
import no.vaccsca.amandman.model.data.dto.IncomingMessageJson
import no.vaccsca.amandman.model.data.dto.MessageToServer
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

class AmanDataClientEuroScope(
    private val host: String,
    private val port: Int,
) : AmanDataClient() {
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null

    private val objectMapper = jacksonObjectMapper().apply {
        // Configure Jackson for large messages
        factory.configure(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING, true)
    }
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    override fun sendMessage(message: MessageToServer) {
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
                        super.handleMessage(dataPackage)
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