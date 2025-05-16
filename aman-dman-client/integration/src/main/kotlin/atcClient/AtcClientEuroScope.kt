package atcClient

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import org.example.integration.entities.IncomingMessageJson
import org.example.integration.entities.MessageToServer
import java.io.*
import java.net.Socket

class AtcClientEuroScope(
    private val host: String,
    private val port: Int,
) : AtcClient() {
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null

    private val objectMapper = jacksonObjectMapper()
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
                        writer = OutputStreamWriter(socket!!.getOutputStream(), Charsets.UTF_8)
                        reader = InputStreamReader(socket!!.getInputStream(), Charsets.UTF_8)
                        isConnected = true
                        println("Connected to $host:$port")

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
            val bufferedReader = BufferedReader(reader)
            while (isConnected) {
                val message = bufferedReader.readLine() ?: break
                val dataPackage = objectMapper.readValue(message, IncomingMessageJson::class.java)
                super.handleMessage(dataPackage)
            }
        } catch (e: Exception) {
            println("Error receiving message: ${e.message}")
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
