package no.vaccsca.amandman.view.airport

import no.vaccsca.amandman.common.NtpClock
import java.awt.FlowLayout
import java.awt.Graphics
import javax.swing.*

class Footer : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("--:--:--")

    init {
        add(timeLabel)


        // Every second, repaint the component
        Timer(1000) {
            repaint()
        }.start()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = NtpClock.now().toString().substring(11, 19)
    }
}