package no.vaccsca.amandman

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class TextAreaAppender(private val jTextArea: JTextArea) : AppenderBase<ILoggingEvent>() {

    override fun append(eventObject: ILoggingEvent) {
        SwingUtilities.invokeLater {
            jTextArea.append(eventObject.formattedMessage + "\n")
        }
    }
}