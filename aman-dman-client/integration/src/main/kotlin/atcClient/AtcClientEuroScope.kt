package atcClient

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.integration.entities.IncomingMessageJson
import org.example.integration.entities.MessageToServer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class AtcClientEuroScope(
    private val host: String,
    private val port: Int,
) : AtcClient() {
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null

    private val objectMapper = jacksonObjectMapper()

    init {
        connect()
    }

    // Method to send a message to the server
    override fun sendMessage(message: MessageToServer) {
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            writer?.write(jsonMessage + "\n")
            writer?.flush()
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
        }
    }

    private fun connect() {
        try {
            socket = Socket(host, port)
            writer = socket?.getOutputStream()?.let { OutputStreamWriter(it, "UTF-8") }
            reader = socket?.getInputStream()?.let { InputStreamReader(it, "UTF-8") }

            // Start a coroutine to handle receiving messages
            CoroutineScope(Dispatchers.IO).launch {
                receiveMessages()
            }
        } catch (e: Exception) {
            println("Error connecting to server: ${e.message}")
        }
    }

    // Method to continuously receive messages and trigger the callback when a message is received
    private fun receiveMessages() {
        try {
            val bufferedReader = BufferedReader(reader)
            while (true) {
                val message = bufferedReader.readLine()
                val dataPackage = objectMapper.readValue(message, IncomingMessageJson::class.java)
                if (message != null) {
                    super.handleMessage(dataPackage)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            println("Error receiving message: ${e.message}")
            e.printStackTrace()
        }
    }

    // Method to close the connection
    fun close() {
        try {
            socket?.close()
            writer?.close()
            reader?.close()
        } catch (e: Exception) {
            println("Error closing connection: ${e.message}")
        }
    }
}
