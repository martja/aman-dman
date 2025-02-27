import java.io.*
import java.net.*
import kotlinx.coroutines.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class TCPJsonClient(
    private val host: String,
    private val port: Int,
    private val onMessageReceived: (String) -> Unit // Callback that gets triggered when a message is received
) {
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null

    private val objectMapper = jacksonObjectMapper()

    init {
        connect()
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

    // Method to send a message to the server
    fun sendMessage(message: Any) {
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            writer?.write(jsonMessage)
            writer?.flush()
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
        }
    }

    // Method to continuously receive messages and trigger the callback when a message is received
    private fun receiveMessages() {
        try {
            val bufferedReader = BufferedReader(reader)
            while (true) {
                val message = bufferedReader.readLine()
                if (message != null) {
                    onMessageReceived(message) // Trigger the callback
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            println("Error receiving message: ${e.message}")
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
