package no.vaccsca.amandman.view.dialogs

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants

/**
 * Data class to hold log event information
 */
data class LogEntry(
    val level: Level,
    val formattedMessage: String
)

/**
 * In-memory appender that stores recent log events for display in the UI.
 */
class InMemoryLogAppender : AppenderBase<ILoggingEvent>() {
    private val logEvents = mutableListOf<LogEntry>()
    private val maxEvents = 5000
    private val listeners = mutableListOf<(LogEntry) -> Unit>()

    private var encoder: PatternLayoutEncoder? = null

    fun setEncoder(encoder: PatternLayoutEncoder) {
        this.encoder = encoder
    }

    override fun append(eventObject: ILoggingEvent) {
        val currentEncoder = encoder ?: return
        val formattedMessage = String(currentEncoder.encode(eventObject))
        val logEntry = LogEntry(eventObject.level, formattedMessage)

        synchronized(logEvents) {
            logEvents.add(logEntry)
            if (logEvents.size > maxEvents) {
                logEvents.removeAt(0)
            }
        }

        // Notify listeners on EDT
        SwingUtilities.invokeLater {
            listeners.forEach { it(logEntry) }
        }
    }

    fun getAllLogs(): List<LogEntry> = synchronized(logEvents) {
        logEvents.toList()
    }

    fun addListener(listener: (LogEntry) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (LogEntry) -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        private var instance: InMemoryLogAppender? = null

        fun getInstance(): InMemoryLogAppender {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        // Check if already configured via logback.xml
                        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
                        val existingAppender = rootLogger.getAppender("IN_MEMORY") as? InMemoryLogAppender

                        instance = existingAppender ?: InMemoryLogAppender().apply {
                            setContext(LoggerFactory.getILoggerFactory() as LoggerContext)
                            start()
                            rootLogger.addAppender(this)
                        }
                    }
                }
            }
            return instance!!
        }
    }
}

/**
 * Dialog that displays application logs in a scrollable text area with color coding by level.
 */
class LogViewerDialog(parent: JFrame) : JDialog(parent, "Application Logs", false) {

    private val document = DefaultStyledDocument()
    private val textPane = JTextPane(document).apply {
        isEditable = false
        font = Font("Monospaced", Font.PLAIN, 12)
    }

    private val scrollPane = JScrollPane(textPane).apply {
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }

    private val clearButton = JButton("Clear Logs").apply {
        addActionListener {
            document.remove(0, document.length)
        }
    }

    private val autoScrollCheckBox = JCheckBox("Auto-scroll", true)

    private val levelFilterComboBox = JComboBox(arrayOf("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR")).apply {
        selectedItem = "ALL"
        addActionListener {
            reloadLogs()
        }
    }

    private val logAppender = InMemoryLogAppender.getInstance()

    // Define text styles for each log level

    private val errorStyle = document.addStyle("ERROR", null).apply {
        StyleConstants.setForeground(this, Color(220, 50, 47)) // Red
        StyleConstants.setBold(this, true)
    }

    private val warnStyle = document.addStyle("WARN", null).apply {
        StyleConstants.setForeground(this, Color(181, 137, 0)) // Yellow/Orange
        StyleConstants.setBold(this, true)
    }

    private val infoStyle = document.addStyle("INFO", null).apply {
        StyleConstants.setForeground(this, Color(38, 139, 210)) // Blue
    }

    private val debugStyle = document.addStyle("DEBUG", null).apply {
        StyleConstants.setForeground(this, Color(133, 153, 0)) // Green
    }

    private val traceStyle = document.addStyle("TRACE", null).apply {
        StyleConstants.setForeground(this, Color(108, 113, 196)) // Violet
    }

    private val defaultStyle = document.addStyle("DEFAULT", null).apply {
        StyleConstants.setForeground(this, Color.BLACK)
    }

    private val logListener: (LogEntry) -> Unit = { logEntry ->
        if (shouldDisplayLog(logEntry)) {
            appendColoredLog(logEntry)
            if (autoScrollCheckBox.isSelected) {
                textPane.caretPosition = document.length
            }
        }
    }

    init {
        layout = BorderLayout()

        // Load existing logs
        reloadLogs()

        // Add listener for new logs
        logAppender.addListener(logListener)

        // Top panel with filter
        val topPanel = JPanel().apply {
            add(JLabel("Level Filter:"))
            add(levelFilterComboBox)
        }

        // Bottom panel with controls
        val bottomPanel = JPanel().apply {
            add(clearButton)
            add(autoScrollCheckBox)
        }

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        preferredSize = Dimension(800, 600)
        setLocationRelativeTo(parent)

        // Clean up listener when dialog is closed
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: java.awt.event.WindowEvent?) {
                logAppender.removeListener(logListener)
            }
        })

        pack()
    }

    private fun shouldDisplayLog(logEntry: LogEntry): Boolean {
        val selectedLevel = levelFilterComboBox.selectedItem as String
        if (selectedLevel == "ALL") return true

        val filterLevel = Level.toLevel(selectedLevel)
        return logEntry.level.isGreaterOrEqual(filterLevel)
    }

    private fun reloadLogs() {
        document.remove(0, document.length)
        val existingLogs = logAppender.getAllLogs()
        existingLogs.forEach { logEntry ->
            if (shouldDisplayLog(logEntry)) {
                appendColoredLog(logEntry)
            }
        }
        if (autoScrollCheckBox.isSelected) {
            textPane.caretPosition = document.length
        }
    }

    private fun appendColoredLog(logEntry: LogEntry) {
        val style = when (logEntry.level.levelInt) {
            Level.ERROR_INT -> errorStyle
            Level.WARN_INT -> warnStyle
            Level.INFO_INT -> infoStyle
            Level.DEBUG_INT -> debugStyle
            Level.TRACE_INT -> traceStyle
            else -> defaultStyle
        }

        try {
            document.insertString(document.length, logEntry.formattedMessage, style)
        } catch (e: Exception) {
            println(e)
            // Ignore errors when inserting text
        }
    }
}

